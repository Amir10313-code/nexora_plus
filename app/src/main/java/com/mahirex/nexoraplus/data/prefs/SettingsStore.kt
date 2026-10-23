package com.mahirex.nexoraplus.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.core.settings.AppSettings
import com.mahirex.nexoraplus.core.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.nexoraDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexora_settings")

/** A focus session that is currently running (persisted so it survives the app being closed). */
data class ActiveFocus(
    val accountId: Long,
    val startedAtMillis: Long,
    val plannedMinutes: Int,
    val dnd: Boolean,
    val previousFilter: Int
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.nexoraDataStore

    private object K {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme_mode")
        val calendarMode = stringPreferencesKey("calendar_mode")
        val region = stringPreferencesKey("occasion_region")
        val hijriCorrection = intPreferencesKey("hijri_correction")
        val secondaryHijri = booleanPreferencesKey("secondary_hijri")
        val weekStart = stringPreferencesKey("week_start")
        val updateUrl = stringPreferencesKey("update_url_template")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val activeAccount = longPreferencesKey("active_account")

        val focusAccount = longPreferencesKey("focus_account")
        val focusStarted = longPreferencesKey("focus_started")
        val focusPlanned = intPreferencesKey("focus_planned")
        val focusDnd = booleanPreferencesKey("focus_dnd")
        val focusPrevFilter = intPreferencesKey("focus_prev_filter")
    }

    private val safeData: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<AppSettings> = safeData.map { p ->
        val defaults = AppSettings()
        AppSettings(
            language = p[K.language] ?: defaults.language,
            themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            calendarMode = p[K.calendarMode] ?: defaults.calendarMode,
            occasionRegion = p[K.region] ?: defaults.occasionRegion,
            hijriCorrection = (p[K.hijriCorrection] ?: 0).coerceIn(DateEngine.MIN_CORRECTION, DateEngine.MAX_CORRECTION),
            showSecondaryHijri = p[K.secondaryHijri] ?: defaults.showSecondaryHijri,
            weekStart = p[K.weekStart] ?: defaults.weekStart,
            updateUrlTemplate = p[K.updateUrl] ?: defaults.updateUrlTemplate,
            autoUpdate = p[K.autoUpdate] ?: defaults.autoUpdate,
            activeAccountId = p[K.activeAccount] ?: defaults.activeAccountId
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setLanguage(value: String) = store.edit { it[K.language] = value }
    suspend fun setThemeMode(value: ThemeMode) = store.edit { it[K.theme] = value.name }
    suspend fun setCalendarMode(value: String) = store.edit { it[K.calendarMode] = value }
    suspend fun setOccasionRegion(value: String) = store.edit { it[K.region] = value }
    suspend fun setHijriCorrection(value: Int) =
        store.edit { it[K.hijriCorrection] = value.coerceIn(DateEngine.MIN_CORRECTION, DateEngine.MAX_CORRECTION) }
    suspend fun setShowSecondaryHijri(value: Boolean) = store.edit { it[K.secondaryHijri] = value }
    suspend fun setWeekStart(value: String) = store.edit { it[K.weekStart] = value }
    suspend fun setUpdateUrlTemplate(value: String) = store.edit { it[K.updateUrl] = value.trim() }
    suspend fun setAutoUpdate(value: Boolean) = store.edit { it[K.autoUpdate] = value }
    suspend fun setActiveAccount(id: Long) = store.edit { it[K.activeAccount] = id }

    // ---- focus session state -------------------------------------------------------------

    val activeFocus: Flow<ActiveFocus?> = safeData.map { p ->
        val started = p[K.focusStarted] ?: return@map null
        ActiveFocus(
            accountId = p[K.focusAccount] ?: 0L,
            startedAtMillis = started,
            plannedMinutes = p[K.focusPlanned] ?: 25,
            dnd = p[K.focusDnd] ?: false,
            previousFilter = p[K.focusPrevFilter] ?: 0
        )
    }

    suspend fun setActiveFocus(value: ActiveFocus) = store.edit {
        it[K.focusAccount] = value.accountId
        it[K.focusStarted] = value.startedAtMillis
        it[K.focusPlanned] = value.plannedMinutes
        it[K.focusDnd] = value.dnd
        it[K.focusPrevFilter] = value.previousFilter
    }

    suspend fun clearActiveFocus() = store.edit {
        it.remove(K.focusAccount)
        it.remove(K.focusStarted)
        it.remove(K.focusPlanned)
        it.remove(K.focusDnd)
        it.remove(K.focusPrevFilter)
    }
}
