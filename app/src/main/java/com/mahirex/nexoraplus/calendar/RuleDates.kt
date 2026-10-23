package com.mahirex.nexoraplus.calendar

import java.time.LocalDate
import java.time.YearMonth

/** Date rules that are not a fixed day in a calendar. */
object RuleDates {
    /** Western (Gregorian) Easter Sunday, "Anonymous Gregorian" algorithm. */
    fun easterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return LocalDate.of(year, month, day)
    }

    /**
     * The [n]-th [isoWeekday] (1 = Monday .. 7 = Sunday) of a Gregorian month.
     * [n] = -1 means the last one. Returns null when it does not exist.
     */
    fun nthWeekday(year: Int, month: Int, isoWeekday: Int, n: Int): LocalDate? {
        if (n == 0 || n < -1 || n > 5) return null
        if (n == -1) {
            val last = YearMonth.of(year, month).atEndOfMonth()
            val diff = Math.floorMod(last.dayOfWeek.value - isoWeekday, 7)
            return last.minusDays(diff.toLong())
        }
        val first = LocalDate.of(year, month, 1)
        val offset = Math.floorMod(isoWeekday - first.dayOfWeek.value, 7)
        val date = first.plusDays(offset + 7L * (n - 1))
        return if (date.monthValue == month) date else null
    }
}
