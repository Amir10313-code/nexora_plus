package com.mahirex.nexoraplus.core

/** Swatches offered for accounts, habits, lists and schedule blocks (ARGB). */
object Palette {
    val swatches: List<Int> = listOf(
        0xFF4F46E5.toInt(), // indigo
        0xFF0F766E.toInt(), // teal
        0xFFDB2777.toInt(), // pink
        0xFFD97706.toInt(), // amber
        0xFF16A34A.toInt(), // green
        0xFF2563EB.toInt(), // blue
        0xFF9333EA.toInt(), // purple
        0xFFDC2626.toInt()  // red
    )

    fun at(index: Int): Int = swatches[Math.floorMod(index, swatches.size)]
}
