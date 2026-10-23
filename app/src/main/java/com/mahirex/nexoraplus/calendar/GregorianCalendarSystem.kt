package com.mahirex.nexoraplus.calendar

import java.time.LocalDate
import java.time.YearMonth

object GregorianCalendarSystem : CalendarSystem {
    override val type = CalendarType.GREGORIAN
    override val minYear = 1900
    override val maxYear = 2200

    override fun fromEpochDay(epochDay: Long): CivilDate {
        val d = LocalDate.ofEpochDay(epochDay)
        return CivilDate(d.year, d.monthValue, d.dayOfMonth)
    }

    override fun toEpochDay(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).toEpochDay()

    override fun monthLength(year: Int, month: Int): Int = YearMonth.of(year, month).lengthOfMonth()

    override fun isLeapYear(year: Int): Boolean = java.time.Year.isLeap(year.toLong())
}
