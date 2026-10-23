package com.mahirex.nexoraplus.data.repo

/** Pure helpers for habit streaks. Days are epoch days. */
object Streaks {
    /**
     * Number of consecutive logged days ending today; when today is not logged yet the streak that
     * ends yesterday still counts (the day is not over).
     */
    fun current(days: Set<Long>, today: Long): Int {
        var cursor = if (today in days) today else today - 1
        var count = 0
        while (cursor in days) {
            count++
            cursor--
        }
        return count
    }

    fun longest(days: Set<Long>): Int {
        var best = 0
        var run = 0
        var prev: Long? = null
        for (d in days.sorted()) {
            run = if (prev != null && d == prev + 1) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    /** Oldest first: whether each of the last [count] days up to [today] is logged. */
    fun lastDays(days: Set<Long>, today: Long, count: Int = 7): List<Boolean> =
        (count - 1 downTo 0).map { (today - it) in days }
}
