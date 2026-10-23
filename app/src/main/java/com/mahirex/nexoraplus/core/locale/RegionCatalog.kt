package com.mahirex.nexoraplus.core.locale

import com.mahirex.nexoraplus.calendar.HijriVariant
import java.time.DayOfWeek
import java.util.Locale

data class RegionInfo(
    /** Lower-case ISO country code, also the dataset id (e.g. "ir"). */
    val id: String,
    /** Base languages spoken in the region (used to decide whether the device country applies). */
    val languages: Set<String>,
    val weekStart: DayOfWeek,
    val weekend: Set<DayOfWeek>,
    val hijriVariant: HijriVariant
) {
    val countryCode: String get() = id.uppercase()

    val flag: String
        get() = String(
            intArrayOf(0x1F1E6 + (countryCode[0] - 'A'), 0x1F1E6 + (countryCode[1] - 'A')),
            0, 2
        )

    fun countryName(locale: Locale): String = Locale("", countryCode).getDisplayCountry(locale)
}

object RegionCatalog {
    const val GLOBAL = "global"
    const val AUTO = "auto"

    private val SAT_SUN = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    val all: List<RegionInfo> = listOf(
        RegionInfo("ir", setOf("fa"), DayOfWeek.SATURDAY, setOf(DayOfWeek.FRIDAY), HijriVariant.TABULAR),
        RegionInfo("sa", setOf("ar"), DayOfWeek.SUNDAY, setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY), HijriVariant.UMM_AL_QURA),
        RegionInfo("tr", setOf("tr"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("pk", setOf("ur", "en"), DayOfWeek.SUNDAY, setOf(DayOfWeek.SUNDAY), HijriVariant.UMM_AL_QURA),
        RegionInfo("in", setOf("hi", "en"), DayOfWeek.SUNDAY, setOf(DayOfWeek.SUNDAY), HijriVariant.UMM_AL_QURA),
        RegionInfo("ru", setOf("ru"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("de", setOf("de"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("fr", setOf("fr"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("es", setOf("es"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("it", setOf("it"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("br", setOf("pt"), DayOfWeek.SUNDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("id", setOf("id"), DayOfWeek.SUNDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("cn", setOf("zh"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("gb", setOf("en"), DayOfWeek.MONDAY, SAT_SUN, HijriVariant.UMM_AL_QURA),
        RegionInfo("us", setOf("en"), DayOfWeek.SUNDAY, SAT_SUN, HijriVariant.UMM_AL_QURA)
    )

    fun byId(id: String): RegionInfo? = all.firstOrNull { it.id == id.lowercase() }

    /**
     * Region used for occasions and week defaults:
     * 1. an explicit choice in settings;
     * 2. the device country, when it is supported and its languages include the app language;
     * 3. the default region of the app language.
     */
    fun resolve(setting: String, language: AppLanguage, deviceCountry: String?): RegionInfo {
        byId(setting)?.let { return it }
        val device = deviceCountry?.takeIf { it.isNotBlank() }?.let { byId(it) }
        if (device != null && language.base in device.languages) return device
        return byId(language.defaultRegion) ?: all[0]
    }
}
