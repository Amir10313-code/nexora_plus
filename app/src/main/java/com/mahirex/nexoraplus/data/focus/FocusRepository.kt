package com.mahirex.nexoraplus.data.focus

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mahirex.nexoraplus.NexoraApp
import com.mahirex.nexoraplus.data.local.NexoraDatabase
import com.mahirex.nexoraplus.data.local.entity.FocusSessionEntity
import com.mahirex.nexoraplus.data.prefs.ActiveFocus
import com.mahirex.nexoraplus.data.prefs.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

/**
 * Deep-focus sessions. The running session is persisted (so closing the app does not lose it) and a
 * WorkManager job ends it on time, which also restores the user's Do-Not-Disturb setting.
 */
class FocusRepository(
    context: Context,
    private val db: NexoraDatabase,
    private val settings: SettingsStore
) {
    private val appContext = context.applicationContext
    private val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val lock = Mutex()

    val active: Flow<ActiveFocus?> = settings.activeFocus

    fun dndAccessGranted(): Boolean = nm.isNotificationPolicyAccessGranted

    suspend fun start(accountId: Long, minutes: Int, useDnd: Boolean): Boolean = lock.withLock {
        if (settings.activeFocus.first() != null) return false
        var previous = NotificationManager.INTERRUPTION_FILTER_ALL
        var dnd = false
        if (useDnd && nm.isNotificationPolicyAccessGranted) {
            previous = nm.currentInterruptionFilter
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            dnd = true
        }
        settings.setActiveFocus(ActiveFocus(accountId, System.currentTimeMillis(), minutes, dnd, previous))
        val request = OneTimeWorkRequestBuilder<FocusEndWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        true
    }

    /** Ends the session now (user pressed stop, or the time is up). Returns the stored session. */
    suspend fun finish(now: Long = System.currentTimeMillis()): FocusSessionEntity? = lock.withLock {
        val a = settings.activeFocus.first() ?: return null
        settings.clearActiveFocus()
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)

        if (a.dnd && nm.isNotificationPolicyAccessGranted) {
            val restore = a.previousFilter.takeIf { it in 1..4 } ?: NotificationManager.INTERRUPTION_FILTER_ALL
            nm.setInterruptionFilter(restore)
        }

        val elapsedMillis = (now - a.startedAtMillis).coerceAtLeast(0)
        val completed = elapsedMillis >= a.plannedMinutes * 60_000L
        val actual = if (completed) a.plannedMinutes else (elapsedMillis / 60_000L).toInt()
        if (!completed && actual < 1) return null

        val session = FocusSessionEntity(
            accountId = a.accountId, startedAt = a.startedAtMillis, plannedMinutes = a.plannedMinutes,
            actualMinutes = actual, dndUsed = a.dnd, completed = completed
        )
        val id = db.focusDao().insert(session)
        session.copy(id = id)
    }

    /** Used by the worker and when the app returns: ends the session only if its time is over. */
    suspend fun finishIfDue(now: Long = System.currentTimeMillis()): FocusSessionEntity? {
        val a = settings.activeFocus.first() ?: return null
        return if (now >= a.startedAtMillis + a.plannedMinutes * 60_000L) finish(now) else null
    }

    private companion object {
        const val WORK_NAME = "focus_end"
    }
}

class FocusEndWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as NexoraApp).container
        container.focusRepository.finishIfDue()
        return Result.success()
    }
}
