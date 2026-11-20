package io.github.alxiw.wearpagingmediator.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity

@Dao
internal interface ReposDao {

    /**
     * Updates existing rows instead of deleting and re-inserting them: `repos` is shared by all
     * search queries and `search_query_results` references it with `ON DELETE CASCADE`, so a
     * replace would drop the repository from the cached results of every other query.
     */
    @Upsert
    suspend fun upsertAll(repos: List<RepoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepoTopics(crossRefs: List<RepoTopicCrossRef>)

    /**
     * A page carries the whole current topic list of its repos, so their links are dropped before
     * the new ones are inserted: otherwise a topic removed on GitHub would stay in the cache.
     */
    @Query("DELETE FROM repo_topic_cross_ref WHERE repo_id IN (:repoIds)")
    suspend fun deleteRepoTopics(repoIds: List<Long>)

    @Query("""
        SELECT topic FROM repo_topic_cross_ref
        WHERE repo_id = :repoId
        ORDER BY topic COLLATE NOCASE ASC
    """)
    suspend fun topicsOf(repoId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(searchResults: List<SearchQueryResultEntity>)

    @Query("SELECT COUNT(*) FROM search_query_results WHERE `query` = :query")
    suspend fun countSearchResults(query: String): Int

    @Query("SELECT COUNT(*) FROM repo_remote_keys WHERE `query` = :query")
    suspend fun countRemoteKeys(query: String): Int

    @Query("SELECT stars FROM repos WHERE id = :repoId")
    suspend fun starsOf(repoId: Long): Int?

    @Query("SELECT position FROM search_query_results WHERE `query` = :query ORDER BY position ASC")
    suspend fun positionsFor(query: String): List<Int>

    @Query("""
        SELECT repos.* FROM repos 
        INNER JOIN search_query_results ON repos.id = search_query_results.repo_id 
        WHERE search_query_results.`query` = :query 
        ORDER BY search_query_results.position ASC
    """)
    fun getReposForQuery(query: String): PagingSource<Int, RepoEntity>

    @Query("DELETE FROM search_query_results WHERE `query` = :query")
    suspend fun deleteResultsForQuery(query: String)

    @Query("DELETE FROM search_query_results WHERE `query` IN (:queries)")
    suspend fun deleteResultsForQueries(queries: List<String>)

    /**
     * Deletes repositories no cached query references anymore. Only rows without any reference are
     * removed: deleting a repository still referenced by another query would cascade into that
     * query's results.
     */
    @Query("""
        DELETE FROM repos
        WHERE NOT EXISTS (SELECT 1 FROM search_query_results WHERE search_query_results.repo_id = repos.id)
        AND NOT EXISTS (SELECT 1 FROM repo_remote_keys WHERE repo_remote_keys.repo_id = repos.id)
    """)
    suspend fun deleteOrphanRepos()

    /** Deletes the topics of the repositories deleted by [deleteOrphanRepos]. */
    @Query("""
        DELETE FROM repo_topic_cross_ref
        WHERE NOT EXISTS (SELECT 1 FROM repos WHERE repos.id = repo_topic_cross_ref.repo_id)
    """)
    suspend fun deleteOrphanRepoTopics()
}
