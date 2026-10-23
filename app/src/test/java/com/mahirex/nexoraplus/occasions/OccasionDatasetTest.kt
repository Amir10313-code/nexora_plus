package com.mahirex.nexoraplus.occasions

import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.calendar.HijriVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class OccasionDatasetTest {
    private val assets = File("src/main/assets/occasions")
    private val regions = listOf("global", "ir", "sa", "tr", "pk", "in", "ru", "de", "fr", "es", "it", "br", "id", "cn", "gb", "us")

    private fun load(region: String): OccasionDataset {
        val text = File(assets, "$region.json").readText()
        val r = OccasionDatasetParser.parse(text, region, 30)
        assertTrue("$region: $r", r is ParseResult.Ok)
        return (r as ParseResult.Ok).dataset
    }

    @Test
    fun everyBundledDatasetIsValid() {
        for (r in regions) {
            val ds = load(r)
            assertEquals(r, ds.regionId)
            assertTrue(ds.occasions.isNotEmpty())
        }
    }

    @Test
    fun parserRejectsBrokenDocuments() {
        fun bad(text: String, region: String = "ir") =
            assertTrue(text, OccasionDatasetParser.parse(text, region, 30) is ParseResult.Invalid)

        bad("not json")
        bad("""{"schema":2,"regionId":"ir","datasetVersion":1,"occasions":[]}""")
        bad("""{"schema":1,"regionId":"de","datasetVersion":1,"occasions":[]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"minAppVersionCode":99,"occasions":[{"id":"a","calendar":"G","month":1,"day":1,"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","calendar":"J","month":13,"day":1,"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","calendar":"J","month":8,"day":31,"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","calendar":"G","month":2,"day":30,"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","calendar":"G","month":1,"day":1,"category":"NATIONAL","titles":{"fa":"x"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","calendar":"G","month":1,"day":1,"category":"WRONG","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","calendar":"G","month":1,"day":1,"category":"NATIONAL","titles":{"fa":"x","en":"y"}},{"id":"a","calendar":"G","month":1,"day":2,"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","rule":{"type":"nthWeekday","month":1,"weekday":9,"n":1},"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
        bad("""{"schema":1,"regionId":"ir","datasetVersion":1,"occasions":[{"id":"a","rule":{"type":"magic"},"category":"NATIONAL","titles":{"fa":"x","en":"y"}}]}""")
    }

    @Test
    fun parserAcceptsAMinimalDocument() {
        val ok = """{"schema":1,"regionId":"IR","datasetVersion":3,"occasions":[{"id":"a","calendar":"J","month":1,"day":1,"category":"NATIONAL","holiday":true,"verified":false,"titles":{"fa":"x","en":"y"}}]}"""
        val r = OccasionDatasetParser.parse(ok, "ir", 30) as ParseResult.Ok
        val def = r.dataset.occasions.single()
        assertEquals(3, r.dataset.version)
        assertEquals(false, def.verified)
        assertEquals(true, def.holiday)
    }

    private fun resolveDay(region: String, engine: DateEngine, date: LocalDate): List<String> {
        val e = date.toEpochDay()
        return OccasionResolver(engine).resolve(load(region).occasions, e, e).map { it.def.id }
    }

    @Test
    fun iranNationalOccasionsFollowTheSolarCalendar() {
        val engine = DateEngine(HijriVariant.TABULAR, 0)
        assertTrue("ir-nowruz-1" in resolveDay("ir", engine, LocalDate.of(2026, 3, 21)))
        assertTrue("ir-nowruz-1" in resolveDay("ir", engine, LocalDate.of(2024, 3, 20)))
        assertTrue("ir-sizdah-bedar" in resolveDay("ir", engine, LocalDate.of(2026, 4, 2)))
        assertTrue("ir-revolution-victory" in resolveDay("ir", engine, LocalDate.of(2026, 2, 11)))
        assertTrue("ir-yalda" in resolveDay("ir", engine, LocalDate.of(2025, 12, 21)))
    }

    @Test
    fun lunarOccasionsMoveWithTheCorrection() {
        val ashura0 = DateEngine(HijriVariant.TABULAR, 0)
        val base = (2026..2026).flatMap { y ->
            (LocalDate.of(y, 6, 1).toEpochDay()..LocalDate.of(y, 7, 31).toEpochDay()).filter { e ->
                OccasionResolver(ashura0).resolve(load("ir").occasions, e, e).any { it.def.id == "ir-ashura" }
            }
        }
        assertEquals(1, base.size)
        for (k in -2..2) {
            val engine = DateEngine(HijriVariant.TABULAR, k)
            val found = (base[0] - 5..base[0] + 5).filter { e ->
                OccasionResolver(engine).resolve(load("ir").occasions, e, e).any { it.def.id == "ir-ashura" }
            }
            assertEquals(listOf(base[0] - k), found)
        }
    }

    @Test
    fun regionalRulesResolveToTheRightDays() {
        val engine = DateEngine()
        assertTrue("us-thanksgiving" in resolveDay("us", engine, LocalDate.of(2026, 11, 26)))
        assertTrue("us-memorial-day" in resolveDay("us", engine, LocalDate.of(2026, 5, 25)))
        assertTrue("de-good-friday" in resolveDay("de", engine, LocalDate.of(2026, 4, 3)))
        assertTrue("de-easter-monday" in resolveDay("de", engine, LocalDate.of(2026, 4, 6)))
        assertTrue("de-ascension" in resolveDay("de", engine, LocalDate.of(2026, 5, 14)))
        assertTrue("de-whit-monday" in resolveDay("de", engine, LocalDate.of(2026, 5, 25)))
        assertTrue("gb-summer-bank" in resolveDay("gb", engine, LocalDate.of(2026, 8, 31)))
    }

    @Test
    fun occasionsOfOtherRegionsAreNeverMixedIn() {
        val engine = DateEngine()
        val germanDay = resolveDay("de", engine, LocalDate.of(2026, 10, 3))
        assertTrue(germanDay.all { it.startsWith("de-") })
        assertTrue(resolveDay("de", engine, LocalDate.of(2026, 3, 21)).none { it.startsWith("ir-") })
    }

    @Test
    fun titlesFallBackToEnglishThenPersian() {
        val def = load("de").occasions.first { it.id == "de-unity-day" }
        assertEquals("Tag der Deutschen Einheit", def.title("de"))
        assertEquals("German Unity Day", def.title("ja"))
        assertEquals("German Unity Day", def.title("ar"))
    }

    @Test
    fun clampKeepsMonthEndOccasionsOnShortMonths() {
        val defs = load("ir").occasions.filter { it.id == "ir-safar-end" }
        val engine = DateEngine(HijriVariant.TABULAR, 0)
        // Over a long period every Safar yields exactly one occurrence, whether it has 29 or 30 days.
        val start = LocalDate.of(2025, 1, 1).toEpochDay()
        val end = LocalDate.of(2027, 12, 31).toEpochDay()
        val found = OccasionResolver(engine).resolve(defs, start, end)
        assertTrue(found.size in 3..4)
    }
}
