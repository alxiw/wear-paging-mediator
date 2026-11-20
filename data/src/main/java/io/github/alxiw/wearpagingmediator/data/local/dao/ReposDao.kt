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
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity

@Dao
internal interface ReposDao {

    /**
     * Updates existing rows instead of deleting and re-inserting them: `repos` is shared by all
     * search queries and `search_query_results` references it with `ON DELETE CASCADE`, so a
     * replace would drop the repository from the cached results of every other query.
     */
    @Upsert
    suspend fun upsertAll(repos: List<RepoEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepoTopics(crossRefs: List<RepoTopicCrossRef>)

    /**
     * A page carries the whole current topic list of its repos, so their links are dropped before
     * the new ones are inserted: otherwise a topic removed on GitHub would stay in the cache.
     */
    @Query("DELETE FROM repo_topic_cross_ref WHERE id IN (:repoIds)")
    suspend fun deleteRepoTopics(repoIds: List<Long>)

    @Query("""
        SELECT topic FROM repo_topic_cross_ref
        WHERE id = :repoId
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
        INNER JOIN search_query_results ON repos.id = search_query_results.repoId 
        WHERE search_query_results.`query` = :query 
        ORDER BY search_query_results.position ASC
    """)
    fun getReposForQuery(query: String): PagingSource<Int, RepoEntity>

    @Query("""
        SELECT DISTINCT repos.* FROM repos 
        LEFT JOIN repo_topic_cross_ref ON repos.id = repo_topic_cross_ref.id 
        WHERE repos.name LIKE :query 
        OR repos.description LIKE :query 
        OR repo_topic_cross_ref.topic LIKE :query 
        ORDER BY stars DESC, name ASC
    """)
    fun searchAllRepos(query: String): PagingSource<Int, RepoEntity>

    @Query("DELETE FROM repos")
    suspend fun deleteAllRepos()

    @Query("DELETE FROM search_query_results WHERE `query` = :query")
    suspend fun deleteResultsForQuery(query: String)

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()

    @Query("DELETE FROM repo_topic_cross_ref")
    suspend fun deleteAllRepoTopics()
}
