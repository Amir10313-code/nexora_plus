package com.mahirex.nexoraplus.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.core.locale.LanguageCatalog
import com.mahirex.nexoraplus.core.locale.LocaleUtil
import com.mahirex.nexoraplus.core.locale.RegionCatalog
import com.mahirex.nexoraplus.core.settings.AppSettings
import com.mahirex.nexoraplus.core.settings.ThemeMode
import com.mahirex.nexoraplus.data.backup.ImportResult
import com.mahirex.nexoraplus.data.local.entity.OccasionDatasetEntity
import com.mahirex.nexoraplus.data.sync.SyncStatus
import com.mahirex.nexoraplus.data.sync.SyncSummary
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.LocalDigits
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SectionTitle
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Space
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.time.DayOfWeek
import java.util.Date

/** A one-off message for a dialog: string resource plus an optional number argument. */
data class UiMessage(val res: Int, val number: Int? = null)

class SettingsViewModel(c: AppContainer) : BaseViewModel(c) {
    val settings = c.settingsStore.settings.asState(AppSettings())
    val datasets = c.calendarRepository.datasets().asState(emptyList<OccasionDatasetEntity>())
    val updating = MutableStateFlow(false)
    val summary = MutableStateFlow<SyncSummary?>(null)
    val message = MutableStateFlow<UiMessage?>(null)

    fun setLanguage(value: String) = viewModelScope.launch {
        c.settingsStore.setLanguage(value)
        LocaleUtil.applyAppLocale(value)
    }
    fun setTheme(v: ThemeMode) = viewModelScope.launch { c.settingsStore.setThemeMode(v) }
    fun setCalendarMode(v: String) = viewModelScope.launch { c.settingsStore.setCalendarMode(v) }
    fun setRegion(v: String) = viewModelScope.launch { c.settingsStore.setOccasionRegion(v) }
    fun setHijriCorrection(v: Int) = viewModelScope.launch { c.settingsStore.setHijriCorrection(v.coerceIn(-2, 2)) }
    fun setSecondaryHijri(v: Boolean) = viewModelScope.launch { c.settingsStore.setShowSecondaryHijri(v) }
    fun setWeekStart(v: String) = viewModelScope.launch { c.settingsStore.setWeekStart(v) }
    fun setUpdateUrl(v: String) = viewModelScope.launch { c.settingsStore.setUpdateUrlTemplate(v) }
    fun setAutoUpdate(v: Boolean) = viewModelScope.launch { c.settingsStore.setAutoUpdate(v) }

    fun updateNow(regionIds: List<String>) {
        if (updating.value) return
        viewModelScope.launch {
            updating.value = true
            try {
                summary.value = c.syncManager.updateNow(regionIds)
            } finally {
                updating.value = false
            }
        }
    }

    fun export(context: Context, uri: Uri, versionName: String) = viewModelScope.launch {
        val ok = try {
            val ids = c.accountRepository.accounts.first().map { it.id }
            val json = c.backupManager.export(ids, versionName)
            withContext(Dispatchers.IO) {
                val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("no stream")
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
        message.value = UiMessage(if (ok) R.string.backup_export_done else R.string.backup_export_failed)
    }

    fun import(context: Context, uri: Uri) = viewModelScope.launch {
        val text = try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (text == null) {
            message.value = UiMessage(R.string.backup_import_invalid)
            return@launch
        }
        message.value = when (val r = c.backupManager.import(text)) {
            is ImportResult.Success -> UiMessage(R.string.backup_import_done, r.accounts)
            ImportResult.InvalidFile -> UiMessage(R.string.backup_import_invalid)
            ImportResult.TooManyAccounts -> UiMessage(R.string.backup_import_too_many)
            ImportResult.UnsupportedVersion -> UiMessage(R.string.backup_import_unsupported)
        }
    }
}

private enum class Picker { LANGUAGE, THEME, CALENDAR, REGION, WEEK_START, UPDATE_URL }

@Composable
fun SettingsScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: SettingsViewModel = viewModel(factory = factory)
    val s by vm.settings.collectAsStateWithLifecycle()
    val datasets by vm.datasets.collectAsStateWithLifecycle()
    val updating by vm.updating.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val profile = LocalProfile.current
    val context = LocalContext.current
    val fmtr = rememberDateFormatter(profile)
    val digits = LocalDigits.current
    var picker by remember { mutableStateOf<Picker?>(null) }

    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.export(context, uri, versionName)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.import(context, uri)
    }

    val languageLabel = if (s.language == LanguageCatalog.SYSTEM) stringResource(R.string.language_device_default)
    else LanguageCatalog.byTag(s.language)?.label ?: s.language
    val themeLabel = stringResource(
        when (s.themeMode) {
            ThemeMode.SYSTEM -> R.string.theme_system
            ThemeMode.LIGHT -> R.string.theme_light
            ThemeMode.DARK -> R.string.theme_dark
        }
    )
    val calendarLabel = calendarModeLabel(s.calendarMode)
    val regionLabel = if (s.occasionRegion == RegionCatalog.AUTO) stringResource(R.string.settings_auto) + " (" + regionName(profile.region.id, profile.locale) + ")"
    else regionName(s.occasionRegion, profile.locale)
    val weekLabel = if (s.weekStart == "auto") stringResource(R.string.settings_auto) + " (" + fmtr.weekdayFull(profile.weekStart) + ")"
    else weekDayLabel(s.weekStart, fmtr.weekdayFull(DayOfWeek.SATURDAY), fmtr.weekdayFull(DayOfWeek.SUNDAY), fmtr.weekdayFull(DayOfWeek.MONDAY))

    ScreenScaffold(title = stringResource(R.string.settings_title), onBack = { nav.popBackStack() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            item { SectionTitle(stringResource(R.string.settings_general)) }
            item {
                LayeredCard {
                    SettingRow(stringResource(R.string.settings_language), languageLabel) { picker = Picker.LANGUAGE }
                    SettingRow(stringResource(R.string.settings_theme), themeLabel) { picker = Picker.THEME }
                }
            }
            item { SectionTitle(stringResource(R.string.settings_calendar)) }
            item {
                LayeredCard(accent = MaterialTheme.colorScheme.secondary) {
                    SettingRow(stringResource(R.string.settings_calendar_type), calendarLabel) { picker = Picker.CALENDAR }
                    SettingRow(stringResource(R.string.settings_region), regionLabel) { picker = Picker.REGION }
                    SettingRow(stringResource(R.string.settings_week_start), weekLabel) { picker = Picker.WEEK_START }
                    Row(Modifier.fillMaxWidth().padding(vertical = Space.half), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_hijri_correction), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.settings_hijri_correction_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(enabled = s.hijriCorrection > -2, onClick = { vm.setHijriCorrection(s.hijriCorrection - 1) }) {
                            Icon(Icons.Rounded.Remove, stringResource(R.string.decrease))
                        }
                        Text(signed(s.hijriCorrection, digits), style = MaterialTheme.typography.titleMedium)
                        IconButton(enabled = s.hijriCorrection < 2, onClick = { vm.setHijriCorrection(s.hijriCorrection + 1) }) {
                            Icon(Icons.Rounded.Add, stringResource(R.string.increase))
                        }
                    }
                    if (profile.language.base in SECONDARY_HIJRI_LANGUAGES && profile.primary == CalendarType.GREGORIAN) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.settings_show_hijri), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Switch(checked = s.showSecondaryHijri, onCheckedChange = { vm.setSecondaryHijri(it) })
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.settings_occasions_update)) }
            item {
                LayeredCard(accent = MaterialTheme.colorScheme.tertiary) {
                    SettingRow(
                        stringResource(R.string.settings_update_url),
                        s.updateUrlTemplate.ifBlank { stringResource(R.string.settings_update_url_empty) }
                    ) { picker = Picker.UPDATE_URL }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_auto_update), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Switch(checked = s.autoUpdate, onCheckedChange = { vm.setAutoUpdate(it) })
                    }
                    Button(
                        enabled = !updating, onClick = { vm.updateNow(profile.occasionRegionIds) },
                        modifier = Modifier.fillMaxWidth().padding(top = Space.s1)
                    ) { Text(stringResource(if (updating) R.string.settings_updating else R.string.settings_update_now)) }
                    summary?.let { sum ->
                        Text(
                            summaryText(sum), style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = Space.half)
                        )
                    }
                    profile.occasionRegionIds.forEach { id ->
                        val d = datasets.firstOrNull { it.regionId == id }
                        if (d != null) {
                            val whenText = d.lastSuccessAt?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, profile.locale).format(Date(it)) }
                            Text(
                                digitsLocalize(
                                    digits,
                                    id.uppercase() + " · v" + d.appliedVersion + " · " + stringResource(sourceRes(d.source)) +
                                        " · " + stringResource(statusRes(d.status)) + (whenText?.let { " · $it" } ?: "")
                                ),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.settings_backup)) }
            item {
                LayeredCard {
                    Text(stringResource(R.string.settings_backup_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = Space.s1), horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                        Button(onClick = { exportLauncher.launch("nexora-backup.json") }, Modifier.weight(1f)) { Text(stringResource(R.string.backup_export)) }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, Modifier.weight(1f)) {
                            Text(stringResource(R.string.backup_import))
                        }
                    }
                }
            }
        }
    }

    when (picker) {
        Picker.LANGUAGE -> {
            val options = buildList {
                add(LanguageCatalog.SYSTEM to stringResource(R.string.language_device_default))
                LanguageCatalog.all.forEach { add(it.tag to it.label) }
            }
            ChoiceDialog(stringResource(R.string.settings_language), options, s.language, { vm.setLanguage(it); picker = null }, { picker = null })
        }
        Picker.THEME -> ChoiceDialog(
            stringResource(R.string.settings_theme),
            listOf(
                ThemeMode.SYSTEM.name to stringResource(R.string.theme_system),
                ThemeMode.LIGHT.name to stringResource(R.string.theme_light),
                ThemeMode.DARK.name to stringResource(R.string.theme_dark)
            ),
            s.themeMode.name, { vm.setTheme(ThemeMode.valueOf(it)); picker = null }, { picker = null }
        )
        Picker.CALENDAR -> ChoiceDialog(
            stringResource(R.string.settings_calendar_type),
            listOf(
                "auto" to stringResource(R.string.settings_auto),
                CalendarType.JALALI.code to stringResource(R.string.calendar_jalali),
                CalendarType.GREGORIAN.code to stringResource(R.string.calendar_gregorian),
                CalendarType.HIJRI.code to stringResource(R.string.calendar_hijri)
            ),
            s.calendarMode, { vm.setCalendarMode(it); picker = null }, { picker = null }
        )
        Picker.REGION -> {
            val options = buildList {
                add(RegionCatalog.AUTO to stringResource(R.string.settings_auto))
                RegionCatalog.all.forEach { add(it.id to regionName(it.id, profile.locale)) }
            }
            ChoiceDialog(stringResource(R.string.settings_region), options, s.occasionRegion, { vm.setRegion(it); picker = null }, { picker = null })
        }
        Picker.WEEK_START -> ChoiceDialog(
            stringResource(R.string.settings_week_start),
            listOf(
                "auto" to stringResource(R.string.settings_auto),
                "SAT" to fmtr.weekdayFull(DayOfWeek.SATURDAY),
                "SUN" to fmtr.weekdayFull(DayOfWeek.SUNDAY),
                "MON" to fmtr.weekdayFull(DayOfWeek.MONDAY)
            ),
            s.weekStart, { vm.setWeekStart(it); picker = null }, { picker = null }
        )
        Picker.UPDATE_URL -> UrlDialog(s.updateUrlTemplate, { vm.setUpdateUrl(it); picker = null }, { picker = null })
        null -> Unit
    }

    message?.let { m ->
        AlertDialog(
            onDismissRequest = { vm.message.value = null },
            text = { Text(if (m.number != null) loc(m.res, m.number) else stringResource(m.res)) },
            confirmButton = { TextButton(onClick = { vm.message.value = null }) { Text(stringResource(R.string.ok)) } }
        )
    }
}

private val SECONDARY_HIJRI_LANGUAGES = setOf("tr", "ur", "id")

private fun regionName(id: String, locale: java.util.Locale): String {
    val info = RegionCatalog.byId(id) ?: return id.uppercase()
    return info.flag + " " + info.countryName(locale)
}

private fun signed(value: Int, digits: com.mahirex.nexoraplus.core.locale.DigitStyle): String =
    digits.localize(if (value > 0) "+$value" else value.toString())

private fun digitsLocalize(digits: com.mahirex.nexoraplus.core.locale.DigitStyle, text: String) = digits.localize(text)

private fun weekDayLabel(code: String, sat: String, sun: String, mon: String) = when (code) {
    "SAT" -> sat
    "SUN" -> sun
    "MON" -> mon
    else -> code
}

@Composable
private fun calendarModeLabel(code: String): String = stringResource(
    when (code) {
        CalendarType.JALALI.code -> R.string.calendar_jalali
        CalendarType.GREGORIAN.code -> R.string.calendar_gregorian
        CalendarType.HIJRI.code -> R.string.calendar_hijri
        else -> R.string.settings_auto
    }
)

private fun sourceRes(source: String) = if (source == "ONLINE") R.string.source_online else R.string.source_bundled

private fun statusRes(status: String) = when (status) {
    SyncStatus.OK -> R.string.sync_status_ok
    SyncStatus.NOT_MODIFIED, SyncStatus.UP_TO_DATE -> R.string.sync_status_up_to_date
    SyncStatus.ERROR -> R.string.sync_status_error
    SyncStatus.INVALID -> R.string.sync_status_invalid
    SyncStatus.NOT_CONFIGURED -> R.string.sync_status_not_configured
    else -> R.string.sync_status_up_to_date
}

@Composable
private fun summaryText(sum: SyncSummary): String = when {
    sum.notConfigured -> stringResource(R.string.sync_summary_not_configured)
    sum.failed > 0 -> loc(R.string.sync_summary_failed, sum.failed) + (sum.firstError?.let { " ($it)" } ?: "")
    sum.updated > 0 -> loc(R.string.sync_summary_updated, sum.updated)
    else -> stringResource(R.string.sync_summary_up_to_date)
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Space.s1)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options, key = { it.first }) { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(value) }.padding(vertical = Space.half),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == selected, onClick = { onSelect(value) })
                        Text(label, Modifier.padding(start = Space.s1))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun UrlDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_update_url)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), placeholder = { Text("https://example.com/occasions/{region}.json") })
                Text(stringResource(R.string.settings_update_url_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
