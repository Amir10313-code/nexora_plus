package com.mahirex.nexoraplus.calendar

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Wraps a calendar and moves it by a constant number of days. Positive [shift] means the calendar's
 * date is *later* than the base calendar's date on the same real day.
 */
class ShiftedCalendarSystem(private val base: CalendarSystem, private val shift: Int) : CalendarSystem {
    override val type get() = base.type
    override val minYear get() = base.minYear
    override val maxYear get() = base.maxYear

    override fun fromEpochDay(epochDay: Long) = base.fromEpochDay(epochDay + shift)
    override fun toEpochDay(year: Int, month: Int, day: Int) = base.toEpochDay(year, month, day) - shift
    override fun monthLength(year: Int, month: Int) = base.monthLength(year, month)
    override fun isLeapYear(year: Int) = base.isLeapYear(year)
}

/** Entry point of the date engine: conversions for all three calendars with the lunar correction applied. */
class DateEngine(
    val hijriVariant: HijriVariant = HijriVariant.TABULAR,
    hijriCorrection: Int = 0
) {
    val hijriCorrection: Int = hijriCorrection.coerceIn(MIN_CORRECTION, MAX_CORRECTION)

    private val gregorian: CalendarSystem = GregorianCalendarSystem
    private val jalali: CalendarSystem = JalaliCalendarSystem
    private val hijri: CalendarSystem = HijriCalendarSystem(hijriVariant).let {
        if (this.hijriCorrection == 0) it else ShiftedCalendarSystem(it, this.hijriCorrection)
    }

    fun system(type: CalendarType): CalendarSystem = when (type) {
        CalendarType.GREGORIAN -> gregorian
        CalendarType.JALALI -> jalali
        CalendarType.HIJRI -> hijri
    }

    fun civil(type: CalendarType, epochDay: Long): CivilDate = system(type).fromEpochDay(epochDay)

    fun toEpochDay(type: CalendarType, date: CivilDate): Long =
        system(type).toEpochDay(date.year, date.month, date.day)

    companion object {
        const val MIN_CORRECTION = -2
        const val MAX_CORRECTION = 2

        fun today(): Long = LocalDate.now().toEpochDay()

        fun dayOfWeek(epochDay: Long): DayOfWeek = LocalDate.ofEpochDay(epochDay).dayOfWeek
    }
}
