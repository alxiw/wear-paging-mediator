package io.github.alxiw.wearpagingmediator.domain.model

import java.time.Instant

/**
 * A query the user opened, together with the moment it was opened: the dialog of the recent
 * queries offers the last [MAX_CACHED_QUERIES] of them, the most recently used one first.
 *
 * [accessedAt] is the moment the query was opened, not the moment GitHub answered, so a query
 * looked at offline still moves to the top of the list.
 */
data class RecentQuery(
    val query: SearchQuery,
    val accessedAt: Instant
) {
    companion object {
        /**
         * How many of the most recently used queries are kept: the database cache keeps the
         * results of that many queries and the dialog offers the same number of them.
         *
         * Must stay above the in-memory cache of `MainViewModel` (3 queries): a query that is
         * still shown from memory is never the least recently used one in the database, so it is
         * not evicted while its window is alive.
         */
        const val MAX_CACHED_QUERIES = 20
    }
}
