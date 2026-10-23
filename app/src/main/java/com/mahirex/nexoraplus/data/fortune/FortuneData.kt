package com.mahirex.nexoraplus.data.fortune

import com.mahirex.nexoraplus.R

/**
 * Couplets (beits) of Hafez of Shiraz (14th century, public domain). The text follows the widely
 * printed Qazvini–Ghani / Khanlari editions; wording differs slightly between editions.
 * The interpretations are original texts of this app (string resources fortune_meaning_N).
 */
object FortuneData {
    /** Two hemistichs per couplet. */
    val couplets: List<Pair<String, String>> = listOf(
        "الا یا ایها الساقی ادر کأساً و ناولها" to "که عشق آسان نمود اول ولی افتاد مشکل‌ها",
        "دوش دیدم که ملایک در میخانه زدند" to "گل آدم بسرشتند و به پیمانه زدند",
        "یوسف گم‌گشته باز آید به کنعان غم مخور" to "کلبهٔ احزان شود روزی گلستان غم مخور",
        "روز وصل دوستداران یاد باد" to "یاد باد آن روزگاران یاد باد",
        "سال‌ها دل طلب جام جم از ما می‌کرد" to "وان‌چه خود داشت ز بیگانه تمنا می‌کرد",
        "صبا به لطف بگو آن غزال رعنا را" to "که سر به کوه و بیابان تو داده‌ای ما را",
        "مژدهٔ وصل تو کو کز سر جان برخیزم" to "طایر قدسم و از دام جهان برخیزم",
        "غلام همت آنم که زیر چرخ کبود" to "ز هر چه رنگ تعلق پذیرد آزاد است",
        "آسایش دو گیتی تفسیر این دو حرف است" to "با دوستان مروت با دشمنان مدارا",
        "بیا تا گل برافشانیم و می در ساغر اندازیم" to "فلک را سقف بشکافیم و طرحی نو دراندازیم",
        "کشتی‌شکستگانیم ای باد شرطه برخیز" to "باشد که بازبینیم دیدار آشنا را",
        "به سر جام جم آنگه نظر توانی کرد" to "که خاک میکده‌ها کحل بصر توانی کرد"
    )

    val meaningRes: List<Int> = listOf(
        R.string.fortune_meaning_1, R.string.fortune_meaning_2, R.string.fortune_meaning_3, R.string.fortune_meaning_4,
        R.string.fortune_meaning_5, R.string.fortune_meaning_6, R.string.fortune_meaning_7, R.string.fortune_meaning_8,
        R.string.fortune_meaning_9, R.string.fortune_meaning_10, R.string.fortune_meaning_11, R.string.fortune_meaning_12
    )

    val quoteRes: List<Int> = listOf(
        R.string.quote_1, R.string.quote_2, R.string.quote_3, R.string.quote_4, R.string.quote_5,
        R.string.quote_6, R.string.quote_7, R.string.quote_8, R.string.quote_9, R.string.quote_10
    )

    init {
        check(couplets.size == meaningRes.size)
    }
}
