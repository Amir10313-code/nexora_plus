package com.mahirex.nexoraplus.core.settings

import com.mahirex.nexoraplus.core.locale.LanguageCatalog
import com.mahirex.nexoraplus.core.locale.RegionCatalog

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything stored in DataStore, as one immutable value. */
data class AppSettings(
    /** [LanguageCatalog.SYSTEM] or a tag from [LanguageCatalog.all]. */
    val language: String = LanguageCatalog.SYSTEM,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** "auto" or a CalendarType code ("G", "J", "H"). */
    val calendarMode: String = "auto",
    /** [RegionCatalog.AUTO] or a region id. */
    val occasionRegion: String = RegionCatalog.AUTO,
    val hijriCorrection: Int = 0,
    val showSecondaryHijri: Boolean = true,
    /** "auto", "SAT", "SUN" or "MON". */
    val weekStart: String = "auto",
    /** Download address with a {region} placeholder, e.g. https://example.org/occasions/{region}.json */
    val updateUrlTemplate: String = "",
    val autoUpdate: Boolean = true,
    val activeAccountId: Long = 0L
)
