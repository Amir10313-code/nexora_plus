package com.mahirex.nexoraplus.data.sync

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.mahirex.nexoraplus.data.local.NexoraDatabase
import com.mahirex.nexoraplus.data.local.entity.OccasionDatasetEntity
import com.mahirex.nexoraplus.data.prefs.SettingsStore
import com.mahirex.nexoraplus.data.repo.OccasionMapper
import com.mahirex.nexoraplus.occasions.OccasionDataset
import com.mahirex.nexoraplus.occasions.OccasionDatasetParser
import com.mahirex.nexoraplus.occasions.ParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

object SyncStatus {
    const val OK = "OK"
    const val NOT_MODIFIED = "NOT_MODIFIED"
    const val UP_TO_DATE = "UP_TO_DATE"
    const val ERROR = "ERROR"
    const val INVALID = "INVALID"
    const val NOT_CONFIGURED = "NOT_CONFIGURED"
}

data class SyncSummary(
    val updated: Int,
    val upToDate: Int,
    val failed: Int,
    val notConfigured: Boolean,
    val firstError: String?
)

/**
 * Keeps the occasion cache (Room) current.
 *
 * Bundled datasets and downloaded datasets go through exactly the same parser/validator and the same
 * atomic "replace region" step. A document that fails validation never touches the stored data.
 */
class OccasionSyncManager(
    private val context: Context,
    private val db: NexoraDatabase,
    private val settings: SettingsStore
) {
    private val dao get() = db.occasionDao()

    private val appVersionCode: Int by lazy {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(info).toInt()
        } catch (_: Exception) {
            Int.MAX_VALUE
        }
    }

    /** Loads the datasets shipped in assets/ when they are newer than what is stored. */
    suspend fun ensureBundled(regionIds: List<String>) = withContext(Dispatchers.IO) {
        for (id in regionIds.distinct()) {
            try {
                val text = context.assets.open("occasions/$id.json").use { it.readBytes().toString(Charsets.UTF_8) }
                val parsed = OccasionDatasetParser.parse(text, id, appVersionCode)
                if (parsed is ParseResult.Ok) {
                    applyIfNewer(parsed.dataset, source = "BUNDLED", etag = null, now = null)
                }
            } catch (_: Exception) {
                // A missing bundled file simply means there is nothing to seed for this region.
            }
        }
    }

    /** Downloads the given regions. Safe to call from the UI or from a worker. */
    suspend fun updateNow(regionIds: List<String>): SyncSummary = withContext(Dispatchers.IO) {
        val template = settings.current().updateUrlTemplate.trim()
        val now = System.currentTimeMillis()
        var updated = 0
        var upToDate = 0
        var failed = 0
        var firstError: String? = null

        if (template.isEmpty() || !template.startsWith("https://")) {
            for (id in regionIds.distinct()) {
                recordStatus(id, SyncStatus.NOT_CONFIGURED, if (template.isEmpty()) null else "https required", now, success = false)
            }
            return@withContext SyncSummary(0, 0, 0, notConfigured = true, firstError = null)
        }

        for (id in regionIds.distinct()) {
            val outcome = fetchRegion(template, id, now)
            when (outcome.status) {
                SyncStatus.OK -> updated++
                SyncStatus.NOT_MODIFIED, SyncStatus.UP_TO_DATE -> upToDate++
                else -> {
                    failed++
                    if (firstError == null) firstError = outcome.message
                }
            }
        }
        SyncSummary(updated, upToDate, failed, notConfigured = false, firstError = firstError)
    }

    private class Outcome(val status: String, val message: String? = null)

    private suspend fun fetchRegion(template: String, regionId: String, now: Long): Outcome {
        val existing = dao.getDataset(regionId)
        val url = template.replace("{region}", regionId)
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15_000
                conn.readTimeout = 20_000
                conn.instanceFollowRedirects = true
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/json")
                existing?.etag?.let { conn.setRequestProperty("If-None-Match", it) }

                when (val code = conn.responseCode) {
                    HttpURLConnection.HTTP_NOT_MODIFIED -> {
                        recordStatus(regionId, SyncStatus.NOT_MODIFIED, null, now, success = true)
                        Outcome(SyncStatus.NOT_MODIFIED)
                    }
                    HttpURLConnection.HTTP_OK -> {
                        val body = readLimited(conn)
                        when (val parsed = OccasionDatasetParser.parse(body, regionId, appVersionCode)) {
                            is ParseResult.Invalid -> {
                                recordStatus(regionId, SyncStatus.INVALID, parsed.reason, now, success = false)
                                Outcome(SyncStatus.INVALID, parsed.reason)
                            }
                            is ParseResult.Ok -> {
                                val applied = applyIfNewer(parsed.dataset, "ONLINE", conn.getHeaderField("ETag"), now)
                                if (applied) Outcome(SyncStatus.OK) else {
                                    recordStatus(regionId, SyncStatus.UP_TO_DATE, null, now, success = true)
                                    Outcome(SyncStatus.UP_TO_DATE)
                                }
                            }
                        }
                    }
                    else -> {
                        recordStatus(regionId, SyncStatus.ERROR, "HTTP $code", now, success = false)
                        Outcome(SyncStatus.ERROR, "HTTP $code")
                    }
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            recordStatus(regionId, SyncStatus.ERROR, msg, now, success = false)
            Outcome(SyncStatus.ERROR, msg)
        }
    }

    private fun readLimited(conn: HttpURLConnection): String {
        val out = ByteArrayOutputStream()
        conn.inputStream.use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                if (out.size() > MAX_BYTES) throw IllegalStateException("response too large")
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    /** Replaces the region only when [dataset] has a higher version than the stored one. */
    private suspend fun applyIfNewer(dataset: OccasionDataset, source: String, etag: String?, now: Long?): Boolean {
        val existing = dao.getDataset(dataset.regionId)
        if (existing != null && dataset.version <= existing.appliedVersion && existing.appliedVersion > 0) return false
        val rows = dataset.occasions.map { OccasionMapper.toEntity(it, dataset.version) }
        val entity = OccasionDatasetEntity(
            regionId = dataset.regionId,
            appliedVersion = dataset.version,
            source = source,
            lastCheckedAt = now ?: existing?.lastCheckedAt,
            lastSuccessAt = now ?: existing?.lastSuccessAt,
            status = SyncStatus.OK,
            message = null,
            etag = etag ?: if (source == "ONLINE") null else existing?.etag
        )
        dao.replaceRegion(dataset.regionId, rows, entity)
        return true
    }

    private suspend fun recordStatus(regionId: String, status: String, message: String?, now: Long, success: Boolean) {
        val existing = dao.getDataset(regionId)
        val updated = (existing ?: OccasionDatasetEntity(regionId, 0, "BUNDLED", null, null, status, null, null)).copy(
            lastCheckedAt = now,
            lastSuccessAt = if (success) now else existing?.lastSuccessAt,
            status = status,
            message = message
        )
        dao.upsertDataset(updated)
    }

    private companion object {
        const val MAX_BYTES = 1_500_000
    }
}
