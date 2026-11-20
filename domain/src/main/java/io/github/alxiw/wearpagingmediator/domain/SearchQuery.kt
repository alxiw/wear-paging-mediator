package io.github.alxiw.wearpagingmediator.domain

import java.util.Locale

/**
 * The canonical form of a search query, shared by everything that has to tell two queries apart:
 * the network request, the database cache and the paging cache of the screen.
 *
 * GitHub search is case insensitive and splits a query into words by whitespace, so `Android`,
 * `android` and `  ANDROID  ` are the same search and have to become the same string. Words are
 * kept separated by a single space: that is the separator the search syntax expects, and a space
 * is just a character for the database, so the stored key stays the query itself. An underscore
 * is not a separator on GitHub, it is a part of a word.
 *
 * [Locale.ROOT] is used because the default locale must not change the result: in Turkish a plain
 * lower case would turn `I` into a dotless `ı`.
 */
object SearchQuery {

    private val WHITESPACE = Regex("\\s+")

    /** Returns [raw] as the canonical query: trimmed, single-spaced and lower case. */
    fun normalize(raw: String): String = raw.trim()
        .replace(WHITESPACE, " ")
        .lowercase(Locale.ROOT)
}
