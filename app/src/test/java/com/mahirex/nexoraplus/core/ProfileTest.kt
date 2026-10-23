package com.mahirex.nexoraplus.core

import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.core.locale.DigitStyle
import com.mahirex.nexoraplus.core.locale.LanguageCatalog
import com.mahirex.nexoraplus.core.locale.ProfileFactory
import com.mahirex.nexoraplus.core.locale.RegionCatalog
import com.mahirex.nexoraplus.core.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

class ProfileTest {
    private fun profile(settings: AppSettings = AppSettings(), locale: Locale) = ProfileFactory.build(settings, locale)

    @Test
    fun persianDeviceGetsSolarCalendarWithLunarAndGregorianAside() {
        val p = profile(locale = Locale("fa", "IR"))
        assertEquals(CalendarType.JALALI, p.primary)
        assertEquals(listOf(CalendarType.HIJRI, CalendarType.GREGORIAN), p.secondary)
        assertEquals("ir", p.region.id)
        assertEquals(DayOfWeek.SATURDAY, p.weekStart)
        assertEquals(DigitStyle.PERSIAN, p.digits)
    }

    @Test
    fun arabicUsesHijriPrimary() {
        val p = profile(locale = Locale("ar", "SA"))
        assertEquals(CalendarType.HIJRI, p.primary)
        assertEquals(listOf(CalendarType.GREGORIAN), p.secondary)
        assertEquals("sa", p.region.id)
        assertEquals(DigitStyle.ARABIC_INDIC, p.digits)
    }

    @Test
    fun secondaryHijriCanBeSwitchedOffForTurkishUrduAndIndonesian() {
        for (lang in listOf("tr", "ur", "id")) {
            val on = profile(AppSettings(showSecondaryHijri = true), Locale(lang))
            val off = profile(AppSettings(showSecondaryHijri = false), Locale(lang))
            assertEquals(CalendarType.GREGORIAN, on.primary)
            assertEquals(listOf(CalendarType.HIJRI), on.secondary)
            assertEquals(emptyList<CalendarType>(), off.secondary)
        }
    }

    @Test
    fun otherLanguagesAreGregorianOnly() {
        for (lang in listOf("en", "de", "fr", "es", "it", "ru", "hi", "pt", "zh")) {
            val p = profile(locale = Locale(lang))
            assertEquals(lang, CalendarType.GREGORIAN, p.primary)
            assertTrue(lang, p.secondary.isEmpty())
        }
    }

    @Test
    fun unsupportedDeviceLanguageFallsBackToEnglish() {
        val p = profile(locale = Locale("ja", "JP"))
        assertEquals("en", p.language.base)
        assertEquals("gb", p.region.id)
    }

    @Test
    fun deviceCountryAppliesOnlyWhenTheLanguageFits() {
        assertEquals("us", profile(locale = Locale("en", "US")).region.id)
        assertEquals("in", profile(locale = Locale("en", "IN")).region.id)
        assertEquals("gb", profile(locale = Locale("en", "AU")).region.id)
        // A Persian-speaking user in Germany must not get German holidays.
        assertEquals("ir", profile(locale = Locale("fa", "DE")).region.id)
        assertEquals("de", profile(locale = Locale("de", "DE")).region.id)
        assertEquals("br", profile(locale = Locale("pt", "PT")).region.id)
        assertEquals("pk", profile(locale = Locale("ur", "IN")).region.id)
    }

    @Test
    fun selectedLanguageOverridesTheDeviceLanguage() {
        val p = profile(AppSettings(language = "de"), Locale("fa", "IR"))
        assertEquals("de", p.language.base)
        assertEquals("de", p.region.id)
        assertEquals(CalendarType.GREGORIAN, p.primary)
    }

    @Test
    fun manualCalendarAndRegionOverrides() {
        val p = profile(
            AppSettings(calendarMode = "G", occasionRegion = "us", weekStart = "MON", hijriCorrection = 1),
            Locale("fa", "IR")
        )
        assertEquals(CalendarType.GREGORIAN, p.primary)
        assertEquals(listOf(CalendarType.JALALI, CalendarType.HIJRI), p.secondary)
        assertEquals("us", p.region.id)
        assertEquals(DayOfWeek.MONDAY, p.weekStart)
        assertEquals(1, p.engine.hijriCorrection)
        assertEquals(listOf("us", RegionCatalog.GLOBAL), p.occasionRegionIds)
    }

    @Test
    fun digitStyles() {
        assertEquals("۱۴۰۵/۰۱/۰۱", DigitStyle.PERSIAN.localize("1405/01/01"))
        assertEquals("٢٠٢٦", DigitStyle.ARABIC_INDIC.localize("2026"))
        assertEquals("2026", DigitStyle.LATIN.localize("2026"))
        assertEquals("a۱b", DigitStyle.PERSIAN.localize("a1b"))
    }

    @Test
    fun fourteenLanguagesAndFlags() {
        assertEquals(14, LanguageCatalog.all.size)
        assertEquals("🇮🇷 فارسی (ایران)", LanguageCatalog.all[0].label)
        assertEquals("🇮🇷", RegionCatalog.byId("ir")!!.flag)
        assertEquals("🇺🇸", RegionCatalog.byId("us")!!.flag)
        assertEquals(setOf("fa", "ar", "ur"), LanguageCatalog.all.filter { it.rtl }.map { it.base }.toSet())
        assertEquals("id", LanguageCatalog.normalizeBase("in"))
    }
}
