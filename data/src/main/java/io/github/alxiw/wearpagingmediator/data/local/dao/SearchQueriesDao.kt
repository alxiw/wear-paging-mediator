package io.github.alxiw.wearpagingmediator.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryEntity

@Dao
internal interface SearchQueriesDao {

    @Upsert
    suspend fun upsert(query: SearchQueryEntity)

    /** The queries older than the [keep] most recent ones, the least recently used one first. */
    @Query("""
        SELECT `query` FROM search_queries
        ORDER BY accessed_at DESC, `query` DESC
        LIMIT -1 OFFSET :keep
    """)
    suspend fun queriesBeyond(keep: Int): List<String>

    @Query("SELECT MAX(accessed_at) FROM search_queries")
    suspend fun lastAccessedAt(): Long?

    /** The [limit] most recently used queries, the most recently used one first. */
    @Query("""
        SELECT * FROM search_queries
        ORDER BY accessed_at DESC, `query` DESC
        LIMIT :limit
    """)
    suspend fun recentQueries(limit: Int): List<SearchQueryEntity>

    @Query("DELETE FROM search_queries WHERE `query` IN (:queries)")
    suspend fun deleteQueries(queries: List<String>)

    @Query("SELECT COUNT(*) FROM search_queries")
    suspend fun countQueries(): Int
}
