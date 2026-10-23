package com.mahirex.nexoraplus.calendar

import java.time.DayOfWeek

data class GridDay(val epochDay: Long, val inMonth: Boolean)

/** Builds the weeks of a month page for any calendar. */
object MonthGrid {
    fun build(
        system: CalendarSystem,
        year: Int,
        month: Int,
        weekStart: DayOfWeek
    ): List<List<GridDay>> {
        val first = system.toEpochDay(year, month, 1)
        val length = system.monthLength(year, month)
        val firstDow = DateEngine.dayOfWeek(first).value
        val offset = Math.floorMod(firstDow - weekStart.value, 7)
        val weekCount = (offset + length + 6) / 7
        val start = first - offset
        return List(weekCount) { w ->
            List(7) { d ->
                val e = start + w * 7L + d
                GridDay(e, e >= first && e < first + length)
            }
        }
    }

    /** Moves (year, month) by [delta] months, wrapping years. */
    fun shiftMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val index = year * 12 + (month - 1) + delta
        return Math.floorDiv(index, 12) to Math.floorMod(index, 12) + 1
    }
}
