package com.mahirex.nexoraplus.data.repo

import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.data.local.entity.OccasionEntity
import com.mahirex.nexoraplus.occasions.OccasionCategory
import com.mahirex.nexoraplus.occasions.OccasionDef
import com.mahirex.nexoraplus.occasions.OccasionRule
import org.json.JSONObject

/** Converts between the validated model and the Room cache rows. */
object OccasionMapper {
    private const val FIXED = "FIXED"
    private const val NTH = "NTH_WEEKDAY"
    private const val EASTER = "EASTER"

    fun toEntity(def: OccasionDef, version: Int): OccasionEntity {
        val titles = JSONObject()
        def.titles.forEach { (k, v) -> titles.put(k, v) }
        var type = FIXED
        var cal: String? = null
        var month = 0
        var day = 0
        var clamp = false
        var weekday = 0
        var nth = 0
        var offset = 0
        when (val r = def.rule) {
            is OccasionRule.Fixed -> {
                cal = r.calendar.code; month = r.month; day = r.day; clamp = r.clamp
            }
            is OccasionRule.NthWeekday -> {
                type = NTH; month = r.month; weekday = r.isoWeekday; nth = r.n
            }
            is OccasionRule.EasterOffset -> {
                type = EASTER; offset = r.days
            }
        }
        return OccasionEntity(
            regionId = def.regionId, defId = def.id, ruleType = type, calendarType = cal, month = month, day = day,
            clamp = clamp, weekday = weekday, nth = nth, offsetDays = offset, category = def.category.code,
            holiday = def.holiday, titlesJson = titles.toString(), verified = def.verified, datasetVersion = version
        )
    }

    fun toDef(e: OccasionEntity): OccasionDef? {
        val rule = when (e.ruleType) {
            FIXED -> OccasionRule.Fixed(CalendarType.fromCode(e.calendarType) ?: return null, e.month, e.day, e.clamp)
            NTH -> OccasionRule.NthWeekday(e.month, e.weekday, e.nth)
            EASTER -> OccasionRule.EasterOffset(e.offsetDays)
            else -> return null
        }
        val category = OccasionCategory.fromCode(e.category) ?: return null
        val titles = LinkedHashMap<String, String>()
        try {
            val o = JSONObject(e.titlesJson)
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = o.opt(k) as? String
                if (v != null) titles[k] = v
            }
        } catch (_: Exception) {
            return null
        }
        return OccasionDef(e.defId, e.regionId, rule, category, e.holiday, titles, e.verified)
    }
}
