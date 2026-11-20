package io.github.alxiw.wearpagingmediator.domain.model

import java.util.Locale

/**
 * The canonical form of a search query, shared by everything that has to tell two queries apart:
 * the network request, the database cache and the paging cache of the screen.
 *
 * The constructor is private, so [of] is the only way to build a value: a raw string cannot reach
 * the network or the cache by accident, it has to be turned into a query at the edge it comes from
 * (the voice input, the saved state). Two values with the same canonical string are equal, which is
 * what makes them the same cache key.
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
@JvmInline
value class SearchQuery private constructor(val value: String) {

    /** A blank query is not a search: a caller builds one only to reject it. */
    val isEmpty: Boolean get() = value.isEmpty()

    companion object {

        private val WHITESPACE = Regex("\\s+")

        /** Returns [raw] as the canonical query: trimmed, single-spaced and lower case. */
        fun of(raw: String): SearchQuery = SearchQuery(
            raw.trim()
                .replace(WHITESPACE, " ")
                .lowercase(Locale.ROOT)
        )
    }
}
