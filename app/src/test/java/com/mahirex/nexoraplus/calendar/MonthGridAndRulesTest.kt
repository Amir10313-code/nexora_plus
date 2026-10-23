package com.mahirex.nexoraplus.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class MonthGridAndRulesTest {
    @Test
    fun farvardin1405StartsOnSaturday() {
        val engine = DateEngine()
        val weeks = MonthGrid.build(engine.system(CalendarType.JALALI), 1405, 1, DayOfWeek.SATURDAY)
        assertEquals(5, weeks.size)
        assertEquals(LocalDate.of(2026, 3, 21).toEpochDay(), weeks[0][0].epochDay)
        assertTrue(weeks[0][0].inMonth)
        assertEquals(31, weeks.flatten().count { it.inMonth })
    }

    @Test
    fun mondayStartAddsLeadingDaysFromThePreviousMonth() {
        val weeks = MonthGrid.build(GregorianCalendarSystem, 2026, 3, DayOfWeek.MONDAY)
        // 1 March 2026 is a Sunday: six leading days.
        assertEquals(LocalDate.of(2026, 2, 23).toEpochDay(), weeks[0][0].epochDay)
        assertEquals(false, weeks[0][0].inMonth)
        assertEquals(31, weeks.flatten().count { it.inMonth })
        assertEquals(6, weeks.size)
        weeks.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun everyWeekStartYieldsFullWeeks() {
        for (ws in DayOfWeek.values()) {
            for (m in 1..12) {
                val weeks = MonthGrid.build(GregorianCalendarSystem, 2026, m, ws)
                assertEquals(DateEngine.dayOfWeek(weeks[0][0].epochDay), ws)
                assertTrue(weeks.size in 4..6)
            }
        }
    }

    @Test
    fun shiftMonthWrapsYears() {
        assertEquals(2027 to 1, MonthGrid.shiftMonth(2026, 12, 1))
        assertEquals(2025 to 12, MonthGrid.shiftMonth(2026, 1, -1))
        assertEquals(2026 to 6, MonthGrid.shiftMonth(2026, 6, 0))
        assertEquals(2024 to 12, MonthGrid.shiftMonth(2026, 12, -24))
    }

    @Test
    fun easterSunday() {
        assertEquals(LocalDate.of(2024, 3, 31), RuleDates.easterSunday(2024))
        assertEquals(LocalDate.of(2025, 4, 20), RuleDates.easterSunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), RuleDates.easterSunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), RuleDates.easterSunday(2027))
        assertEquals(LocalDate.of(2038, 4, 25), RuleDates.easterSunday(2038))
    }

    @Test
    fun nthWeekday() {
        assertEquals(LocalDate.of(2026, 11, 26), RuleDates.nthWeekday(2026, 11, 4, 4)) // US Thanksgiving
        assertEquals(LocalDate.of(2026, 1, 19), RuleDates.nthWeekday(2026, 1, 1, 3))   // MLK day
        assertEquals(LocalDate.of(2026, 5, 25), RuleDates.nthWeekday(2026, 5, 1, -1))  // last Monday of May
        assertEquals(LocalDate.of(2026, 9, 7), RuleDates.nthWeekday(2026, 9, 1, 1))    // Labor day
        assertNull(RuleDates.nthWeekday(2026, 2, 1, 5))
    }
}
