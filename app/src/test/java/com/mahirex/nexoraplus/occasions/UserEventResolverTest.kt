package com.mahirex.nexoraplus.occasions

import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.calendar.DateEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class UserEventResolverTest {
    private val engine = DateEngine()
    private fun e(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun yearlyEventInTheSolarCalendarFollowsItsOwnDate() {
        // 1 Farvardin every year: 21 March 2026 and 20 March 2027? (1406 starts 21 March 2027), 1407 starts 20 March 2028.
        val ev = UserEventDef(1, "x", "", CalendarType.JALALI, null, 1, 1, true)
        val found = UserEventResolver.resolve(listOf(ev), engine, e(2026, 1, 1), e(2028, 12, 31)).map { it.epochDay }
        assertEquals(listOf(e(2026, 3, 21), e(2027, 3, 21), e(2028, 3, 20)), found)
    }

    @Test
    fun oneOffEventAppearsOnlyOnItsDay() {
        val ev = UserEventDef(2, "x", "", CalendarType.GREGORIAN, 2026, 5, 5, false)
        assertEquals(1, UserEventResolver.resolve(listOf(ev), engine, e(2026, 5, 1), e(2026, 5, 31)).size)
        assertEquals(0, UserEventResolver.resolve(listOf(ev), engine, e(2027, 5, 1), e(2027, 5, 31)).size)
    }

    @Test
    fun feb29EventFallsBackToFeb28InCommonYears() {
        val ev = UserEventDef(3, "x", "", CalendarType.GREGORIAN, null, 2, 29, true)
        val found = UserEventResolver.resolve(listOf(ev), engine, e(2026, 1, 1), e(2028, 12, 31)).map { it.epochDay }
        assertEquals(listOf(e(2026, 2, 28), e(2027, 2, 28), e(2028, 2, 29)), found)
    }

    @Test
    fun invalidOneOffDatesAreIgnored() {
        val ev = UserEventDef(4, "x", "", CalendarType.JALALI, 1404, 12, 30, false) // 1404 is not leap
        assertEquals(0, UserEventResolver.resolve(listOf(ev), engine, e(2025, 1, 1), e(2026, 12, 31)).size)
    }
}
