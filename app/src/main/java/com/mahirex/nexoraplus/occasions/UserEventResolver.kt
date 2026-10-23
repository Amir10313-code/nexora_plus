package com.mahirex.nexoraplus.occasions

import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.calendar.DateEngine

/** A user-created occasion, independent of the database representation. */
data class UserEventDef(
    val id: Long,
    val title: String,
    val note: String,
    val calendar: CalendarType,
    val year: Int?,
    val month: Int,
    val day: Int,
    val repeatYearly: Boolean
)

data class ResolvedUserEvent(val def: UserEventDef, val epochDay: Long)

object UserEventResolver {
    /** Events are tied to the date in the calendar they were entered in; yearly ones repeat in that calendar. */
    fun resolve(events: List<UserEventDef>, engine: DateEngine, startEpoch: Long, endEpoch: Long): List<ResolvedUserEvent> {
        if (endEpoch < startEpoch) return emptyList()
        val out = ArrayList<ResolvedUserEvent>()
        for (ev in events) {
            val system = engine.system(ev.calendar)
            if (ev.repeatYearly || ev.year == null) {
                FixedDateFinder.find(system, ev.month, ev.day, true, startEpoch, endEpoch)
                    .forEach { out.add(ResolvedUserEvent(ev, it)) }
            } else {
                try {
                    if (system.isValid(ev.year, ev.month, ev.day)) {
                        val e = system.toEpochDay(ev.year, ev.month, ev.day)
                        if (e in startEpoch..endEpoch) out.add(ResolvedUserEvent(ev, e))
                    }
                } catch (_: RuntimeException) {
                    // outside the supported range
                }
            }
        }
        out.sortWith(compareBy({ it.epochDay }, { it.def.id }))
        return out
    }
}
