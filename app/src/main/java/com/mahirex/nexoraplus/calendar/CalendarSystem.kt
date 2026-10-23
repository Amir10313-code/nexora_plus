package com.mahirex.nexoraplus.calendar

/** The three calendars supported by the app. [code] is what is stored in the database / JSON. */
enum class CalendarType(val code: String) {
    GREGORIAN("G"),
    JALALI("J"),
    HIJRI("H");

    companion object {
        fun fromCode(code: String?): CalendarType? = entries.firstOrNull { it.code == code }
    }
}

/** A date in some calendar. The calendar itself is known from context. */
data class CivilDate(val year: Int, val month: Int, val day: Int)

/**
 * A calendar system converts between its own (year, month, day) and the universal
 * [epochDay] (days since 1970-01-01, same as java.time.LocalDate.toEpochDay).
 */
interface CalendarSystem {
    val type: CalendarType

    /** Smallest / largest year the UI is allowed to navigate to. */
    val minYear: Int
    val maxYear: Int

    fun fromEpochDay(epochDay: Long): CivilDate
    fun toEpochDay(year: Int, month: Int, day: Int): Long
    fun monthLength(year: Int, month: Int): Int
    fun isLeapYear(year: Int): Boolean

    fun isValid(year: Int, month: Int, day: Int): Boolean =
        year in minYear..maxYear && month in 1..12 && day >= 1 && day <= monthLength(year, month)
}
