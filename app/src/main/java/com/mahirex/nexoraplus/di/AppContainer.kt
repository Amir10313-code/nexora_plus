package com.mahirex.nexoraplus.di

import android.app.Application
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.core.locale.CalendarProfile
import com.mahirex.nexoraplus.core.locale.LocaleUtil
import com.mahirex.nexoraplus.core.locale.ProfileFactory
import com.mahirex.nexoraplus.core.locale.RegionCatalog
import com.mahirex.nexoraplus.core.settings.AppSettings
import com.mahirex.nexoraplus.data.backup.BackupManager
import com.mahirex.nexoraplus.data.focus.FocusRepository
import com.mahirex.nexoraplus.data.local.NexoraDatabase
import com.mahirex.nexoraplus.data.prefs.SettingsStore
import com.mahirex.nexoraplus.data.repo.AccountRepository
import com.mahirex.nexoraplus.data.repo.CalendarRepository
import com.mahirex.nexoraplus.data.repo.ChecklistRepository
import com.mahirex.nexoraplus.data.repo.FocusHistoryRepository
import com.mahirex.nexoraplus.data.repo.FortuneRepository
import com.mahirex.nexoraplus.data.repo.PlannerRepository
import com.mahirex.nexoraplus.data.repo.ScheduleRepository
import com.mahirex.nexoraplus.data.repo.SearchRepository
import com.mahirex.nexoraplus.data.sync.OccasionSyncManager
import com.mahirex.nexoraplus.data.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Hand-written dependency container (no code generation). */
class AppContainer(private val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsStore = SettingsStore(app)
    val db: NexoraDatabase = NexoraDatabase.build(app)

    val accountRepository = AccountRepository(db, settingsStore)
    val plannerRepository = PlannerRepository(db)
    val checklistRepository = ChecklistRepository(db)
    val calendarRepository = CalendarRepository(db)
    val scheduleRepository = ScheduleRepository(db)
    val fortuneRepository = FortuneRepository(db)
    val searchRepository = SearchRepository(db)
    val focusHistoryRepository = FocusHistoryRepository(db)
    val focusRepository = FocusRepository(app, db, settingsStore)
    val syncManager = OccasionSyncManager(app, db, settingsStore)
    val backupManager = BackupManager(db, settingsStore)

    /** null until DataStore has delivered the first value (avoids a flash of default theme/language). */
    val settingsState: StateFlow<AppSettings?> =
        settingsStore.settings.map<AppSettings, AppSettings?> { it }.stateIn(scope, SharingStarted.Eagerly, null)

    val profile: StateFlow<CalendarProfile> = settingsStore.settings
        .map { ProfileFactory.build(it, LocaleUtil.systemLocale()) }
        .stateIn(scope, SharingStarted.Eagerly, ProfileFactory.build(AppSettings(), LocaleUtil.systemLocale()))

    fun start() {
        scope.launch {
            val language = profile.value.language
            val context = LocaleUtil.localizedContext(app, language)
            accountRepository.ensureDefault(context.getString(R.string.default_account_name))
            val all = RegionCatalog.all.map { it.id } + RegionCatalog.GLOBAL
            syncManager.ensureBundled(all)
        }
        scope.launch {
            settingsStore.settings
                .map { it.autoUpdate && it.updateUrlTemplate.isNotBlank() }
                .distinctUntilChanged()
                .collect { SyncScheduler.apply(app, it) }
        }
    }
}
