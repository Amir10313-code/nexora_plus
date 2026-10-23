package com.mahirex.nexoraplus.occasions

import com.mahirex.nexoraplus.calendar.CalendarType
import org.json.JSONException
import org.json.JSONObject

sealed class ParseResult {
    data class Ok(val dataset: OccasionDataset) : ParseResult()
    data class Invalid(val reason: String) : ParseResult()
}

/**
 * Parses and strictly validates an occasions JSON document (bundled or downloaded).
 * Any problem rejects the whole document so a broken file can never replace good data.
 */
object OccasionDatasetParser {
    const val SUPPORTED_SCHEMA = 1
    const val MAX_OCCASIONS = 2000
    const val MAX_TEXT_LENGTH = 200

    private val VALID_LANG = Regex("^[a-z]{2,3}$")
    private val MAX_GREGORIAN_DAYS = intArrayOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

    fun parse(text: String, expectedRegionId: String, appVersionCode: Int): ParseResult {
        val root = try {
            JSONObject(text)
        } catch (e: JSONException) {
            return invalid("not valid JSON")
        }

        val schema = int(root, "schema") ?: return invalid("schema missing")
        if (schema != SUPPORTED_SCHEMA) return invalid("unsupported schema $schema")

        val regionId = (root.opt("regionId") as? String)?.trim()?.lowercase()
            ?: return invalid("regionId missing")
        if (regionId != expectedRegionId.lowercase()) return invalid("regionId $regionId does not match $expectedRegionId")

        val version = int(root, "datasetVersion") ?: return invalid("datasetVersion missing")
        if (version < 1) return invalid("datasetVersion must be >= 1")

        val minApp = if (root.has("minAppVersionCode")) {
            int(root, "minAppVersionCode") ?: return invalid("minAppVersionCode must be a number")
        } else 0
        if (minApp > appVersionCode) return invalid("requires a newer app version ($minApp)")

        val publishedAt = root.opt("publishedAt") as? String

        val array = root.optJSONArray("occasions") ?: return invalid("occasions array missing")
        if (array.length() == 0) return invalid("occasions is empty")
        if (array.length() > MAX_OCCASIONS) return invalid("too many occasions")

        val defs = ArrayList<OccasionDef>(array.length())
        val seen = HashSet<String>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: return invalid("occasion #$i is not an object")
            val def = when (val r = parseOccasion(o, regionId, i)) {
                is OccasionOrError.Error -> return invalid(r.reason)
                is OccasionOrError.Value -> r.def
            }
            if (!seen.add(def.id)) return invalid("duplicate id ${def.id}")
            defs.add(def)
        }
        return ParseResult.Ok(OccasionDataset(schema, regionId, version, publishedAt, minApp, defs))
    }

    private sealed class OccasionOrError {
        data class Value(val def: OccasionDef) : OccasionOrError()
        data class Error(val reason: String) : OccasionOrError()
    }

    private fun parseOccasion(o: JSONObject, regionId: String, index: Int): OccasionOrError {
        fun err(msg: String) = OccasionOrError.Error("occasion #$index: $msg")

        val id = (o.opt("id") as? String)?.trim()
        if (id.isNullOrEmpty() || id.length > 64) return err("bad id")

        val category = OccasionCategory.fromCode(o.opt("category") as? String) ?: return err("$id: bad category")

        val holiday = if (o.has("holiday")) (o.opt("holiday") as? Boolean ?: return err("$id: holiday must be boolean")) else false
        val verified = if (o.has("verified")) (o.opt("verified") as? Boolean ?: return err("$id: verified must be boolean")) else true

        val titlesObj = o.optJSONObject("titles") ?: return err("$id: titles missing")
        val titles = LinkedHashMap<String, String>()
        val keys = titlesObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = titlesObj.opt(k) as? String ?: return err("$id: title $k must be text")
            if (!VALID_LANG.matches(k)) return err("$id: bad language key $k")
            val t = v.trim()
            if (t.isEmpty() || t.length > MAX_TEXT_LENGTH) return err("$id: bad title for $k")
            titles[k] = t
        }
        if (!titles.containsKey("fa") || !titles.containsKey("en")) return err("$id: titles need fa and en")

        val rule = when (val ruleObj = o.opt("rule")) {
            null, JSONObject.NULL -> {
                val cal = CalendarType.fromCode(o.opt("calendar") as? String) ?: return err("$id: bad calendar")
                val month = int(o, "month") ?: return err("$id: month missing")
                val day = int(o, "day") ?: return err("$id: day missing")
                val clamp = if (o.has("clamp")) (o.opt("clamp") as? Boolean ?: return err("$id: clamp must be boolean")) else false
                if (!validFixed(cal, month, day)) return err("$id: invalid date $month/$day")
                OccasionRule.Fixed(cal, month, day, clamp)
            }
            is JSONObject -> {
                when (ruleObj.opt("type") as? String) {
                    "nthWeekday" -> {
                        val month = int(ruleObj, "month") ?: return err("$id: rule.month missing")
                        val weekday = int(ruleObj, "weekday") ?: return err("$id: rule.weekday missing")
                        val n = int(ruleObj, "n") ?: return err("$id: rule.n missing")
                        if (month !in 1..12 || weekday !in 1..7 || !(n == -1 || n in 1..5)) return err("$id: bad nthWeekday rule")
                        OccasionRule.NthWeekday(month, weekday, n)
                    }
                    "easterOffset" -> {
                        val days = int(ruleObj, "days") ?: return err("$id: rule.days missing")
                        if (days !in -100..100) return err("$id: easterOffset out of range")
                        OccasionRule.EasterOffset(days)
                    }
                    else -> return err("$id: unknown rule type")
                }
            }
            else -> return err("$id: rule must be an object")
        }

        return OccasionOrError.Value(OccasionDef(id, regionId, rule, category, holiday, titles, verified))
    }

    private fun validFixed(cal: CalendarType, month: Int, day: Int): Boolean {
        if (month !in 1..12 || day < 1) return false
        return when (cal) {
            CalendarType.GREGORIAN -> day <= MAX_GREGORIAN_DAYS[month - 1]
            CalendarType.JALALI -> day <= if (month <= 6) 31 else 30
            CalendarType.HIJRI -> day <= 30
        }
    }

    private fun int(o: JSONObject, key: String): Int? = when (val v = o.opt(key)) {
        is Int -> v
        is Long -> if (v in Int.MIN_VALUE..Int.MAX_VALUE) v.toInt() else null
        else -> null
    }

    private fun invalid(reason: String) = ParseResult.Invalid(reason)
}
