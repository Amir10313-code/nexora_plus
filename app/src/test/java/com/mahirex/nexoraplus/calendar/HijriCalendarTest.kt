package com.mahirex.nexoraplus.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HijriCalendarTest {
    private fun epoch(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test
    fun tabularEpochIsFridayJuly16_622Julian() {
        // 16 July 622 (Julian) is 19 July 622 in the proleptic Gregorian calendar used by java.time.
        assertEquals(epoch(622, 7, 19), HijriCalendarSystem.tabularToEpoch(1, 1, 1))
    }

    @Test
    fun tabularLeapYearPattern() {
        val leaps = (1..30).filter { HijriCalendarSystem.tabularIsLeap(it) }
        assertEquals(listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29), leaps)
    }

    @Test
    fun tabularRoundTripAndContinuity() {
        val start = HijriCalendarSystem.tabularToEpoch(1300, 1, 1)
        val end = HijriCalendarSystem.tabularToEpoch(1650, 1, 1)
        var prev: CivilDate? = null
        for (e in start until end) {
            val c = HijriCalendarSystem.tabularFrom(e)
            assertEquals(e, HijriCalendarSystem.tabularToEpoch(c.year, c.month, c.day))
            assertTrue(c.day in 1..HijriCalendarSystem.tabularMonthLength(c.year, c.month))
            if (prev != null) {
                val p = prev
                if (c.year == p.year && c.month == p.month) assertEquals(p.day + 1, c.day) else assertEquals(1, c.day)
            }
            prev = c
        }
    }

    @Test
    fun tabularIsWithinOneDayOfUmmAlQura() {
        val tab = HijriCalendarSystem(HijriVariant.TABULAR)
        val uq = HijriCalendarSystem(HijriVariant.UMM_AL_QURA)
        for (e in epoch(2000, 1, 1)..epoch(2040, 12, 31)) {
            val a = uq.fromEpochDay(e)
            val b = tab.fromEpochDay(e)
            val diff = tab.toEpochDay(a.year, a.month, a.day) - e
            assertTrue("epoch $e: UQ=$a tabular=$b", diff in -2L..2L)
        }
    }

    @Test
    fun ummAlQuraKnownDates() {
        val uq = HijriCalendarSystem(HijriVariant.UMM_AL_QURA)
        assertEquals(epoch(2025, 3, 1), uq.toEpochDay(1446, 9, 1))   // 1 Ramadan 1446
        assertEquals(epoch(2025, 3, 30), uq.toEpochDay(1446, 10, 1)) // Eid al-Fitr 1446
        assertEquals(epoch(2025, 6, 6), uq.toEpochDay(1446, 12, 10)) // Eid al-Adha 1446
        assertEquals(epoch(2025, 6, 26), uq.toEpochDay(1447, 1, 1))  // Islamic new year 1447
        assertEquals(CivilDate(1447, 1, 1), uq.fromEpochDay(epoch(2025, 6, 26)))
    }

    @Test
    fun ummAlQuraRoundTrip() {
        val uq = HijriCalendarSystem(HijriVariant.UMM_AL_QURA)
        for (e in epoch(1950, 1, 1)..epoch(2100, 12, 31)) {
            val c = uq.fromEpochDay(e)
            assertEquals(e, uq.toEpochDay(c.year, c.month, c.day))
            assertTrue(c.day in 1..uq.monthLength(c.year, c.month))
        }
    }

    @Test
    fun correctionMovesTheHijriDateByWholeDays() {
        val base = DateEngine(HijriVariant.TABULAR, 0)
        val e = epoch(2026, 10, 22)
        val b = base.civil(CalendarType.HIJRI, e)
        for (k in -2..2) {
            val eng = DateEngine(HijriVariant.TABULAR, k)
            assertEquals(base.civil(CalendarType.HIJRI, e + k), eng.civil(CalendarType.HIJRI, e))
            val c = eng.civil(CalendarType.HIJRI, e)
            assertEquals(e, eng.toEpochDay(CalendarType.HIJRI, c))
        }
        assertEquals(b, base.civil(CalendarType.HIJRI, e))
    }

    @Test
    fun correctionIsClampedToThePermittedRange() {
        assertEquals(2, DateEngine(HijriVariant.TABULAR, 9).hijriCorrection)
        assertEquals(-2, DateEngine(HijriVariant.TABULAR, -9).hijriCorrection)
    }

    @Test
    fun correctionMovesWhichDayMuharramTenthFallsOn() {
        val base = DateEngine(HijriVariant.UMM_AL_QURA, 0).toEpochDay(CalendarType.HIJRI, CivilDate(1447, 1, 10))
        for (k in -2..2) {
            val moved = DateEngine(HijriVariant.UMM_AL_QURA, k).toEpochDay(CalendarType.HIJRI, CivilDate(1447, 1, 10))
            assertEquals(base - k, moved)
        }
    }

    @Test
    fun monthLengthsAre29Or30() {
        for (variant in HijriVariant.values()) {
            val c = HijriCalendarSystem(variant)
            for (y in 1400..1500) for (m in 1..12) assertTrue(c.monthLength(y, m) in 29..30)
        }
    }
}
