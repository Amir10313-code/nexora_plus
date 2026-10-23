package com.mahirex.nexoraplus.core.locale

import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.core.settings.AppSettings
import java.time.DayOfWeek
import java.util.Locale

/** Everything calendar related that depends on language, region and settings. */
data class CalendarProfile(
    val language: AppLanguage,
    val region: RegionInfo,
    val primary: CalendarType,
    val secondary: List<CalendarType>,
    val weekStart: DayOfWeek,
    val weekend: Set<DayOfWeek>,
    val engine: DateEngine
) {
    val digits: DigitStyle get() = language.digits
    val locale: Locale get() = Locale.forLanguageTag(language.tag)

    /** Dataset ids whose occasions are shown: the region and the worldwide set. */
    val occasionRegionIds: List<String> get() = listOf(region.id, RegionCatalog.GLOBAL)

    /** All calendars in display order: primary first. */
    val allCalendars: List<CalendarType> get() = listOf(primary) + secondary
}

object ProfileFactory {

    private fun defaultOrder(base: String, showSecondaryHijri: Boolean): List<CalendarType> = when (base) {
        "fa" -> listOf(CalendarType.JALALI, CalendarType.HIJRI, CalendarType.GREGORIAN)
        "ar" -> listOf(CalendarType.HIJRI, CalendarType.GREGORIAN)
        "tr", "ur", "id" ->
            if (showSecondaryHijri) listOf(CalendarType.GREGORIAN, CalendarType.HIJRI)
            else listOf(CalendarType.GREGORIAN)
        else -> listOf(CalendarType.GREGORIAN)
    }

    fun parseWeekStart(value: String): DayOfWeek? = when (value) {
        "SAT" -> DayOfWeek.SATURDAY
        "SUN" -> DayOfWeek.SUNDAY
        "MON" -> DayOfWeek.MONDAY
        else -> null
    }

    fun build(settings: AppSettings, systemLocale: Locale): CalendarProfile {
        val language = LanguageCatalog.effective(settings.language, systemLocale)
        val region = RegionCatalog.resolve(settings.occasionRegion, language, systemLocale.country)

        val defaults = defaultOrder(language.base, settings.showSecondaryHijri)
        val chosen = CalendarType.fromCode(settings.calendarMode)
        val primary = chosen ?: defaults.first()
        val secondary = defaults.filter { it != primary }

        return CalendarProfile(
            language = language,
            region = region,
            primary = primary,
            secondary = secondary,
            weekStart = parseWeekStart(settings.weekStart) ?: region.weekStart,
            weekend = region.weekend,
            engine = DateEngine(region.hijriVariant, settings.hijriCorrection)
        )
    }
}
