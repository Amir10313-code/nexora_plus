package com.mahirex.nexoraplus.data

import com.mahirex.nexoraplus.data.repo.TextUtil
import com.mahirex.nexoraplus.data.repo.Streaks
import org.junit.Assert.assertEquals
import org.junit.Test

class StreaksTest {
    @Test
    fun currentStreakCountsBackFromTodayOrYesterday() {
        assertEquals(3, Streaks.current(setOf(8L, 9L, 10L), 10L))
        assertEquals(3, Streaks.current(setOf(8L, 9L, 10L), 11L)) // today not logged yet
        assertEquals(0, Streaks.current(setOf(8L, 9L, 10L), 12L))
        assertEquals(1, Streaks.current(setOf(1L, 2L, 10L), 10L))
        assertEquals(0, Streaks.current(emptySet(), 10L))
    }

    @Test
    fun longestStreak() {
        assertEquals(4, Streaks.longest(setOf(1L, 2L, 3L, 4L, 9L, 10L)))
        assertEquals(0, Streaks.longest(emptySet()))
        assertEquals(1, Streaks.longest(setOf(5L)))
    }

    @Test
    fun lastDaysIsOldestFirst() {
        assertEquals(listOf(false, false, false, false, true, false, true), Streaks.lastDays(setOf(8L, 10L), 10L))
    }

    @Test
    fun likeEscaping() {
        assertEquals("100\\%", TextUtil.escapeLike("100%"))
        assertEquals("a\\_b", TextUtil.escapeLike("a_b"))
        assertEquals("c\\\\d", TextUtil.escapeLike("c\\d"))
    }
}
