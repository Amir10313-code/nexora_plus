package com.mahirex.nexoraplus.calendar

import java.time.LocalDate

/**
 * Solar Hijri (Jalali / Persian) calendar.
 *
 * Uses the well known break-table algorithm (33-year rule with corrections) that follows the
 * official Iranian calendar for the whole supported range. Integer division and remainder
 * intentionally use Kotlin's truncating semantics, exactly as the reference algorithm does.
 */
object JalaliCalendarSystem : CalendarSystem {
    override val type = CalendarType.JALALI
    override val minYear = 1278
    override val maxYear = 1578

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
        1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    private class Cal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int): Cal {
        val bl = breaks.size
        require(jy >= breaks[0] && jy < breaks[bl - 1]) { "Invalid Jalali year $jy" }
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        var i = 1
        while (i < bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += (jump / 33) * 8 + (jump % 33) / 4
            jp = jm
            i++
        }
        var n = jy - jp
        leapJ += (n / 33) * 8 + ((n % 33) + 3) / 4
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = gy / 4 - ((gy / 100 + 1) * 3) / 4 - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = (((n + 1) % 33) - 1) % 4
        if (leap == -1) leap = 4
        return Cal(leap, gy, march)
    }

    override fun isLeapYear(year: Int): Boolean = jalCal(year).leap == 0

    override fun monthLength(year: Int, month: Int): Int = when {
        month <= 6 -> 31
        month <= 11 -> 30
        else -> if (isLeapYear(year)) 30 else 29
    }

    override fun toEpochDay(year: Int, month: Int, day: Int): Long {
        val r = jalCal(year)
        val nowruz = LocalDate.of(r.gy, 3, r.march).toEpochDay()
        return nowruz + (month - 1) * 31L - (month / 7) * (month - 7L) + day - 1
    }

    override fun fromEpochDay(epochDay: Long): CivilDate {
        val gy = LocalDate.ofEpochDay(epochDay).year
        var jy = gy - 621
        val r = jalCal(jy)
        val firstOfYear = LocalDate.of(gy, 3, r.march).toEpochDay()
        var k = epochDay - firstOfYear
        if (k >= 0) {
            if (k <= 185) {
                return CivilDate(jy, 1 + (k / 31).toInt(), (k % 31).toInt() + 1)
            }
            k -= 186
        } else {
            jy -= 1
            k += 179
            if (r.leap == 1) k += 1
        }
        return CivilDate(jy, 7 + (k / 30).toInt(), (k % 30).toInt() + 1)
    }
}
