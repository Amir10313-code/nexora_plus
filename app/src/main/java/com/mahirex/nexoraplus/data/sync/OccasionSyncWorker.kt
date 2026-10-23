package com.mahirex.nexoraplus.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mahirex.nexoraplus.NexoraApp
import java.util.concurrent.TimeUnit

class OccasionSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as NexoraApp).container
        val settings = container.settingsStore.current()
        if (!settings.autoUpdate || settings.updateUrlTemplate.isBlank()) return Result.success()
        val profile = container.profile.value
        val summary = container.syncManager.updateNow(profile.occasionRegionIds)
        return if (summary.failed > 0) Result.retry() else Result.success()
    }
}

object SyncScheduler {
    private const val NAME = "occasion_sync"

    fun apply(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<OccasionSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
