package com.mahirex.nexoraplus.occasions

import com.mahirex.nexoraplus.calendar.CalendarType

enum class OccasionCategory(val code: String) {
    NATIONAL("NATIONAL"),
    RELIGIOUS("RELIGIOUS"),
    GLOBAL("GLOBAL"),
    OFFICIAL("OFFICIAL_HOLIDAY");

    companion object {
        fun fromCode(code: String?): OccasionCategory? = entries.firstOrNull { it.code == code }
    }
}

/** When an occasion happens. Fixed dates are tied to a date in their *own* calendar. */
sealed class OccasionRule {
    /** [month]/[day] of [calendar]; with [clamp] a day past the end of a short month moves to the last day. */
    data class Fixed(
        val calendar: CalendarType,
        val month: Int,
        val day: Int,
        val clamp: Boolean = false
    ) : OccasionRule()

    /** n-th [isoWeekday] (1 = Monday .. 7 = Sunday) of a Gregorian [month]; n = -1 means the last one. */
    data class NthWeekday(val month: Int, val isoWeekday: Int, val n: Int) : OccasionRule()

    /** Western Easter Sunday plus [days]. */
    data class EasterOffset(val days: Int) : OccasionRule()
}

data class OccasionDef(
    val id: String,
    val regionId: String,
    val rule: OccasionRule,
    val category: OccasionCategory,
    val holiday: Boolean,
    /** Language code (2-letter base) to title. "fa" and "en" are always present. */
    val titles: Map<String, String>,
    val verified: Boolean = true
) {
    fun title(language: String): String =
        titles[language] ?: titles["en"] ?: titles["fa"] ?: titles.values.firstOrNull() ?: id
}

data class ResolvedOccasion(val def: OccasionDef, val epochDay: Long)

data class OccasionDataset(
    val schema: Int,
    val regionId: String,
    val version: Int,
    val publishedAt: String?,
    val minAppVersionCode: Int,
    val occasions: List<OccasionDef>
)
