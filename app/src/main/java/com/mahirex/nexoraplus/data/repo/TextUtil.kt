package com.mahirex.nexoraplus.data.repo

object TextUtil {
    /** Escapes LIKE wildcards; queries use `ESCAPE '\'`. */
    fun escapeLike(q: String): String = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
