package io.github.alxiw.wearpagingmediator.domain.model

import java.time.Instant

/**
 * A query the user opened, together with the moment it was opened: the dialog of the recent
 * queries offers the last [MAX_CACHED_QUERIES] of them, the most recently used one first.
 *
 * [accessedAt] is the moment the results of the query were started, not the moment GitHub
 * answered, so an opening made offline still moves the query to the top. A query whose window is
 * kept in memory is shown again without a new Pager, so it keeps its place and its moment.
 */
data class RecentQuery(
    val query: SearchQuery,
    val accessedAt: Instant
) {
    companion object {
        /**
         * How many of the most recently used queries are kept: the database cache keeps the
         * results of that many queries and the dialog offers the same number of them. Must stay
         * above the in-memory cache of `MainViewModel` (3 queries), so a window that is still
         * shown from memory is never evicted from the database.
         */
        const val MAX_CACHED_QUERIES = 20
    }
}
