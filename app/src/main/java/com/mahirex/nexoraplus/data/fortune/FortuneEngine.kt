package com.mahirex.nexoraplus.data.fortune

/** Deterministic daily pick: the same account and day always give the same fortune and quote. */
object FortuneEngine {
    fun indexFor(accountId: Long, epochDay: Long, size: Int, salt: Long = 0L): Int {
        require(size > 0)
        var x = epochDay * -7046029254386353131L + accountId * -4658895280553007687L + salt * -7723592293110705685L
        x = (x xor (x ushr 30)) * -4658895280553007687L
        x = (x xor (x ushr 27)) * -7723592293110705685L
        x = x xor (x ushr 31)
        return Math.floorMod(x, size.toLong()).toInt()
    }

    fun fortuneIndex(accountId: Long, epochDay: Long, size: Int): Int = indexFor(accountId, epochDay, size, 0L)
    fun quoteIndex(accountId: Long, epochDay: Long, size: Int): Int = indexFor(accountId, epochDay, size, 1L)
}
