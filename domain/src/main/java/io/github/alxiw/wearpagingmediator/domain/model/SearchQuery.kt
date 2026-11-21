package io.github.alxiw.wearpagingmediator.domain.model

import java.util.Locale

/**
 * The canonical form of a search query, shared by the network request, the database cache and the
 * paging cache of the screen: two values with the same string are equal, which is what makes them
 * the same cache key.
 *
 * The constructor is private, so [of] is the only way to build a value: raw text has to be turned
 * into a query at the edge it comes from (the voice input, the saved state) and cannot reach the
 * network or the cache unnormalized. GitHub search is case insensitive and splits a query into
 * words by whitespace, so `Android`, `android` and `  ANDROID  ` are the same search; the words are
 * kept separated by a single space, and an underscore is a part of a word.
 *
 * [Locale.ROOT] keeps the result independent of the locale: in Turkish a plain lower case would
 * turn `I` into a dotless `ı`.
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
