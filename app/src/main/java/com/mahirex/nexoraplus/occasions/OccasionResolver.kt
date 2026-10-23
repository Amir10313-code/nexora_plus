package com.mahirex.nexoraplus.occasions

import com.mahirex.nexoraplus.calendar.CalendarSystem
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.calendar.RuleDates
import java.time.LocalDate

/** Finds the epoch days of a "month/day" in a calendar that fall inside a range. */
object FixedDateFinder {
    fun find(
        system: CalendarSystem,
        month: Int,
        day: Int,
        clamp: Boolean,
        startEpoch: Long,
        endEpoch: Long
    ): List<Long> {
        if (month !in 1..12 || day < 1) return emptyList()
        val firstYear: Int
        val lastYear: Int
        try {
            firstYear = system.fromEpochDay(startEpoch).year
            lastYear = system.fromEpochDay(endEpoch).year
        } catch (_: RuntimeException) {
            return emptyList()
        }
        val result = ArrayList<Long>(2)
        for (year in firstYear..lastYear) {
            try {
                val length = system.monthLength(year, month)
                val d = when {
                    day <= length -> day
                    clamp -> length
                    else -> continue
                }
                val e = system.toEpochDay(year, month, d)
                if (e in startEpoch..endEpoch) result.add(e)
            } catch (_: RuntimeException) {
                // year outside what the calendar supports: nothing to show
            }
        }
        return result
    }
}

/** Resolves occasion rules to concrete days using the current [DateEngine] (so the lunar correction applies). */
class OccasionResolver(private val engine: DateEngine) {

    fun resolve(defs: List<OccasionDef>, startEpoch: Long, endEpoch: Long): List<ResolvedOccasion> {
        if (endEpoch < startEpoch) return emptyList()
        val out = ArrayList<ResolvedOccasion>()
        val gFirst = LocalDate.ofEpochDay(startEpoch).year
        val gLast = LocalDate.ofEpochDay(endEpoch).year
        for (def in defs) {
            when (val rule = def.rule) {
                is OccasionRule.Fixed -> {
                    FixedDateFinder.find(
                        engine.system(rule.calendar), rule.month, rule.day, rule.clamp, startEpoch, endEpoch
                    ).forEach { out.add(ResolvedOccasion(def, it)) }
                }
                is OccasionRule.NthWeekday -> {
                    for (y in gFirst..gLast) {
                        if (rule.month !in 1..12 || rule.isoWeekday !in 1..7) continue
                        val d = RuleDates.nthWeekday(y, rule.month, rule.isoWeekday, rule.n) ?: continue
                        val e = d.toEpochDay()
                        if (e in startEpoch..endEpoch) out.add(ResolvedOccasion(def, e))
                    }
                }
                is OccasionRule.EasterOffset -> {
                    for (y in gFirst..gLast) {
                        val e = RuleDates.easterSunday(y).toEpochDay() + rule.days
                        if (e in startEpoch..endEpoch) out.add(ResolvedOccasion(def, e))
                    }
                }
            }
        }
        out.sortWith(
            compareBy<ResolvedOccasion> { it.epochDay }
                .thenBy { if (it.def.holiday) 0 else 1 }
                .thenBy { it.def.category.ordinal }
                .thenBy { it.def.id }
        )
        return out
    }

    fun groupByDay(defs: List<OccasionDef>, startEpoch: Long, endEpoch: Long): Map<Long, List<ResolvedOccasion>> =
        resolve(defs, startEpoch, endEpoch).groupBy { it.epochDay }
}
