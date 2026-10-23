package com.mahirex.nexoraplus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.calendar.CivilDate
import com.mahirex.nexoraplus.core.locale.CalendarProfile
import java.time.DayOfWeek
import java.time.Month
import java.time.format.TextStyle

/** Formats dates for any of the three calendars in the current language and digits. */
class DateFormatter(
    private val profile: CalendarProfile,
    private val jalaliMonths: Array<String>,
    private val hijriMonths: Array<String>
) {
    private val locale = profile.locale
    private val digits = profile.digits

    fun monthName(type: CalendarType, month: Int): String = when (type) {
        CalendarType.GREGORIAN -> Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)
        CalendarType.JALALI -> jalaliMonths[month - 1]
        CalendarType.HIJRI -> hijriMonths[month - 1]
    }

    fun weekdayShort(dow: DayOfWeek): String = dow.getDisplayName(TextStyle.SHORT, locale)
    fun weekdayFull(dow: DayOfWeek): String = dow.getDisplayName(TextStyle.FULL, locale)

    fun number(n: Int): String = digits.format(n)

    fun dayMonthYear(type: CalendarType, d: CivilDate): String =
        digits.localize("${d.day} ${monthName(type, d.month)} ${d.year}")

    fun monthYear(type: CalendarType, year: Int, month: Int): String =
        digits.localize("${monthName(type, month)} $year")

    /** "Saturday, 21 March 2026" style text for an epoch day in calendar [type]. */
    fun full(epochDay: Long, type: CalendarType): String {
        val civil = profile.engine.civil(type, epochDay)
        val dow = java.time.LocalDate.ofEpochDay(epochDay).dayOfWeek
        val comma = if (profile.language.rtl) "،" else ","
        return "${weekdayFull(dow)}$comma ${dayMonthYear(type, civil)}"
    }

    fun short(epochDay: Long, type: CalendarType = profile.primary): String =
        dayMonthYear(type, profile.engine.civil(type, epochDay))

    fun minuteOfDay(minutes: Int): String = digits.localize(String.format(java.util.Locale.ROOT, "%02d:%02d", minutes / 60, minutes % 60))

    fun timeRange(start: Int, end: Int): String = "${minuteOfDay(start)}–${minuteOfDay(end)}"
}

@Composable
fun rememberDateFormatter(profile: CalendarProfile): DateFormatter {
    val jalali = stringArrayResource(R.array.jalali_months)
    val hijri = stringArrayResource(R.array.hijri_months)
    return remember(profile, jalali, hijri) { DateFormatter(profile, jalali, hijri) }
}
