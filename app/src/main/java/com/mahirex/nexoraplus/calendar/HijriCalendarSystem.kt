package com.mahirex.nexoraplus.calendar

import java.time.DateTimeException
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.temporal.ChronoField

/** Which algorithm provides the underlying lunar Hijri dates. */
enum class HijriVariant {
    /** Arithmetic (civil, 30-year cycle) calendar. Predictable, may differ from sighting by a day. */
    TABULAR,

    /** Umm al-Qura via java.time; falls back to [TABULAR] outside the supported range. */
    UMM_AL_QURA
}

/**
 * Lunar Hijri calendar. The real-world date depends on moon sighting, so the app additionally applies
 * a user "day correction" (see [ShiftedCalendarSystem]).
 */
class HijriCalendarSystem(private val variant: HijriVariant) : CalendarSystem {
    override val type = CalendarType.HIJRI
    override val minYear = 1318
    override val maxYear = 1620

    override fun fromEpochDay(epochDay: Long): CivilDate {
        if (variant == HijriVariant.UMM_AL_QURA) {
            val r = umAlQuraFrom(epochDay)
            if (r != null) return r
        }
        return tabularFrom(epochDay)
    }

    override fun toEpochDay(year: Int, month: Int, day: Int): Long {
        if (variant == HijriVariant.UMM_AL_QURA) {
            try {
                return HijrahChronology.INSTANCE.date(year, month, day).toEpochDay()
            } catch (_: DateTimeException) {
                // fall through to the tabular calendar
            }
        }
        return tabularToEpoch(year, month, day)
    }

    override fun monthLength(year: Int, month: Int): Int {
        if (variant == HijriVariant.UMM_AL_QURA) {
            try {
                return HijrahChronology.INSTANCE.date(year, month, 1).lengthOfMonth()
            } catch (_: DateTimeException) {
                // fall through
            }
        }
        return tabularMonthLength(year, month)
    }

    override fun isLeapYear(year: Int): Boolean = monthLength(year, 12) == 30

    companion object {
        /** Julian day number of the Unix epoch (1970-01-01). */
        private const val JDN_UNIX_EPOCH = 2440588L

        /** Julian day number of 1 Muharram 1 AH in the civil (Friday) epoch convention. */
        private const val CIVIL_EPOCH_JDN = 1948440L

        fun tabularIsLeap(year: Int): Boolean = Math.floorMod(11 * year + 14, 30) < 11

        fun tabularMonthLength(year: Int, month: Int): Int = when {
            month % 2 == 1 -> 30
            month == 12 -> if (tabularIsLeap(year)) 30 else 29
            else -> 29
        }

        fun tabularToEpoch(year: Int, month: Int, day: Int): Long {
            val jdn = Math.floorDiv(11L * year + 3, 30L) + 354L * year + 30L * month -
                Math.floorDiv(month - 1L, 2L) + day + CIVIL_EPOCH_JDN - 385L
            return jdn - JDN_UNIX_EPOCH
        }

        fun tabularFrom(epochDay: Long): CivilDate {
            val jdn = epochDay + JDN_UNIX_EPOCH
            var y = Math.floorDiv(30L * (jdn - CIVIL_EPOCH_JDN) + 10646L, 10631L).toInt()
            // Safety net so the result is always consistent with tabularToEpoch.
            while (epochDay < tabularToEpoch(y, 1, 1)) y--
            while (epochDay >= tabularToEpoch(y + 1, 1, 1)) y++
            var rem = (epochDay - tabularToEpoch(y, 1, 1)).toInt()
            var m = 1
            while (m < 12 && rem >= tabularMonthLength(y, m)) {
                rem -= tabularMonthLength(y, m)
                m++
            }
            return CivilDate(y, m, rem + 1)
        }

        private fun umAlQuraFrom(epochDay: Long): CivilDate? = try {
            val d = HijrahChronology.INSTANCE.date(LocalDate.ofEpochDay(epochDay))
            CivilDate(
                d.get(ChronoField.YEAR),
                d.get(ChronoField.MONTH_OF_YEAR),
                d.get(ChronoField.DAY_OF_MONTH)
            )
        } catch (_: DateTimeException) {
            null
        }
    }
}
