package io.github.alxiw.wearpagingmediator.data

import androidx.room.withTransaction
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryEntity
import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import javax.inject.Inject

/**
 * Bounds the cache: the results of at most [RecentQuery.MAX_CACHED_QUERIES] queries are kept,
 * the least recently used ones are deleted together with the repositories no other query
 * references.
 *
 * A query is used when it is opened, not when GitHub answers: the cached results are shown even
 * without a network, and a failed refresh must not make a query that was just looked at look like
 * the least recently used one. A touched query gets an [SearchQueryEntity.accessedAt] above every
 * stored one, so the usage order stays strict even if the clock steps back.
 */
internal class RecentQueriesCache @Inject constructor(
    private val database: GitHubDatabase
) {

    /** Makes [query] the most recently used one and evicts the queries beyond the limit. */
    suspend fun touch(query: String) {
        database.withTransaction {
            val queries = database.searchQueriesDao()
            val accessedAt = maxOf(
                System.currentTimeMillis(),
                (queries.lastAccessedAt() ?: 0L) + 1
            )
            queries.upsert(SearchQueryEntity(query = query, accessedAt = accessedAt))

            val evicted = queries.queriesBeyond(RecentQuery.MAX_CACHED_QUERIES)
            if (evicted.isEmpty()) return@withTransaction
            evicted.chunked(MAX_QUERIES_PER_STATEMENT).forEach { chunk ->
                database.reposDao().deleteResultsForQueries(chunk)
                database.remoteKeysDao().deleteForQueries(chunk)
                queries.deleteQueries(chunk)
            }
            database.reposDao().deleteOrphanRepos()
            database.reposDao().deleteOrphanRepoTopics()
        }
    }

    companion object {
        /** SQLite allows 999 bound parameters per statement. */
        private const val MAX_QUERIES_PER_STATEMENT = 500
    }
}