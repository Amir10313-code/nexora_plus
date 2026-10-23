package com.mahirex.nexoraplus.data.repo

import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.data.local.entity.UserEventEntity
import com.mahirex.nexoraplus.occasions.UserEventDef

fun UserEventEntity.toDef(): UserEventDef? {
    val type = CalendarType.fromCode(calendarType) ?: return null
    return UserEventDef(id, title, note, type, year, month, day, repeatYearly)
}
