package com.mahirex.nexoraplus.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JalaliCalendarTest {
    private val j = JalaliCalendarSystem

    private fun epoch(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun nowruzDatesMatchTheOfficialCalendar() {
        val expected = mapOf(
            1357 to LocalDate.of(1978, 3, 21),
            1368 to LocalDate.of(1989, 3, 21),
            1399 to LocalDate.of(2020, 3, 20),
            1400 to LocalDate.of(2021, 3, 21),
            1401 to LocalDate.of(2022, 3, 21),
            1402 to LocalDate.of(2023, 3, 21),
            1403 to LocalDate.of(2024, 3, 20),
            1404 to LocalDate.of(2025, 3, 21),
            1405 to LocalDate.of(2026, 3, 21),
            1406 to LocalDate.of(2027, 3, 21),
            1407 to LocalDate.of(2028, 3, 20)
        )
        for ((jy, g) in expected) {
            assertEquals("1 Farvardin $jy", g.toEpochDay(), j.toEpochDay(jy, 1, 1))
            assertEquals(CivilDate(jy, 1, 1), j.fromEpochDay(g.toEpochDay()))
        }
    }

    @Test
    fun knownHistoricalDates() {
        // 22 Bahman 1357 = 11 February 1979, 14 Khordad 1368 = 4 June 1989.
        assertEquals(epoch(1979, 2, 11), j.toEpochDay(1357, 11, 22))
        assertEquals(epoch(1989, 6, 4), j.toEpochDay(1368, 3, 14))
        assertEquals(CivilDate(1405, 1, 1), j.fromEpochDay(epoch(2026, 3, 21)))
        assertEquals(CivilDate(1404, 12, 29), j.fromEpochDay(epoch(2026, 3, 20)))
    }

    @Test
    fun leapYearsFollowTheIranianPattern() {
        val expected = listOf(1370, 1375, 1379, 1383, 1387, 1391, 1395, 1399, 1403, 1408, 1412, 1416, 1420, 1424, 1428, 1432, 1436, 1441, 1445)
        val actual = (1370..1445).filter { j.isLeapYear(it) }
        assertEquals(expected, actual)
    }

    @Test
    fun monthLengths() {
        for (y in 1300..1500) {
            for (m in 1..6) assertEquals(31, j.monthLength(y, m))
            for (m in 7..11) assertEquals(30, j.monthLength(y, m))
            assertEquals(if (j.isLeapYear(y)) 30 else 29, j.monthLength(y, 12))
        }
        assertTrue(j.isValid(1403, 12, 30))
        assertFalse(j.isValid(1404, 12, 30))
        assertFalse(j.isValid(1404, 13, 1))
        assertFalse(j.isValid(1404, 0, 1))
    }

    @Test
    fun yearLengthsAre365Or366AndConsecutive() {
        for (y in j.minYear until j.maxYear) {
            val len = j.toEpochDay(y + 1, 1, 1) - j.toEpochDay(y, 1, 1)
            assertEquals("year $y", if (j.isLeapYear(y)) 366L else 365L, len)
            val sum = (1..12).sumOf { j.monthLength(y, it) }.toLong()
            assertEquals(len, sum)
        }
    }

    @Test
    fun roundTripOverTheWholeSupportedRange() {
        val start = j.toEpochDay(j.minYear, 1, 1)
        val end = j.toEpochDay(j.maxYear, 12, j.monthLength(j.maxYear, 12))
        var prev: CivilDate? = null
        for (e in start..end) {
            val c = j.fromEpochDay(e)
            assertEquals("epoch $e -> $c", e, j.toEpochDay(c.year, c.month, c.day))
            assertTrue("valid $c", j.isValid(c.year, c.month, c.day))
            if (prev != null) {
                val p = prev
                val sameMonth = c.year == p.year && c.month == p.month
                if (sameMonth) assertEquals(p.day + 1, c.day)
                else assertEquals(1, c.day)
            }
            prev = c
        }
    }
}
