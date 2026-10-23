package com.mahirex.nexoraplus.core.locale

import java.util.Locale

enum class DigitStyle(private val zero: Char) {
    LATIN('0'),
    ARABIC_INDIC('٠'),
    PERSIAN('۰');

    /** Replaces every ASCII digit in [text] by the digit of this style. */
    fun localize(text: String): String {
        if (this == LATIN) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            if (ch in '0'..'9') sb.append(zero + (ch - '0')) else sb.append(ch)
        }
        return sb.toString()
    }

    fun format(number: Int): String = localize(number.toString())
    fun format(number: Long): String = localize(number.toString())
}

data class AppLanguage(
    /** BCP-47 tag given to the per-app locale API. */
    val tag: String,
    /** Two-letter language used for resources, titles in datasets, etc. */
    val base: String,
    val flag: String,
    /** Native name with the country, e.g. "فارسی (ایران)". */
    val nativeName: String,
    val rtl: Boolean,
    val digits: DigitStyle,
    val defaultRegion: String
) {
    val label: String get() = "$flag $nativeName"
}

object LanguageCatalog {
    const val SYSTEM = "system"

    val all: List<AppLanguage> = listOf(
        AppLanguage("fa", "fa", "🇮🇷", "فارسی (ایران)", true, DigitStyle.PERSIAN, "ir"),
        AppLanguage("en", "en", "🇬🇧", "English (United Kingdom)", false, DigitStyle.LATIN, "gb"),
        AppLanguage("ar", "ar", "🇸🇦", "العربية (السعودية)", true, DigitStyle.ARABIC_INDIC, "sa"),
        AppLanguage("tr", "tr", "🇹🇷", "Türkçe (Türkiye)", false, DigitStyle.LATIN, "tr"),
        AppLanguage("ur", "ur", "🇵🇰", "اردو (پاکستان)", true, DigitStyle.PERSIAN, "pk"),
        AppLanguage("hi", "hi", "🇮🇳", "हिन्दी (भारत)", false, DigitStyle.LATIN, "in"),
        AppLanguage("ru", "ru", "🇷🇺", "Русский (Россия)", false, DigitStyle.LATIN, "ru"),
        AppLanguage("de", "de", "🇩🇪", "Deutsch (Deutschland)", false, DigitStyle.LATIN, "de"),
        AppLanguage("fr", "fr", "🇫🇷", "Français (France)", false, DigitStyle.LATIN, "fr"),
        AppLanguage("es", "es", "🇪🇸", "Español (España)", false, DigitStyle.LATIN, "es"),
        AppLanguage("it", "it", "🇮🇹", "Italiano (Italia)", false, DigitStyle.LATIN, "it"),
        AppLanguage("pt-BR", "pt", "🇧🇷", "Português (Brasil)", false, DigitStyle.LATIN, "br"),
        AppLanguage("id", "id", "🇮🇩", "Bahasa Indonesia (Indonesia)", false, DigitStyle.LATIN, "id"),
        AppLanguage("zh-CN", "zh", "🇨🇳", "中文 (中国)", false, DigitStyle.LATIN, "cn")
    )

    val english: AppLanguage = all[1]

    fun byTag(tag: String): AppLanguage? = all.firstOrNull { it.tag.equals(tag, ignoreCase = true) }

    fun byBase(base: String): AppLanguage? = all.firstOrNull { it.base == normalizeBase(base) }

    /** Old Java codes: "in" -> "id", "iw" -> "he" ... only the ones we care about. */
    fun normalizeBase(base: String): String = when (base.lowercase()) {
        "in" -> "id"
        else -> base.lowercase()
    }

    /**
     * The language the UI is shown in. [selected] is the stored setting ([SYSTEM] or a tag);
     * unsupported device languages fall back to English.
     */
    fun effective(selected: String, systemLocale: Locale): AppLanguage {
        if (selected != SYSTEM) return byTag(selected) ?: english
        return byBase(systemLocale.language) ?: english
    }
}
