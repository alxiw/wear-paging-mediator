package io.github.alxiw.wearpagingmediator.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity

@Dao
internal interface ReposDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<RepoEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepoTopics(crossRefs: List<RepoTopicCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchResults(searchResults: List<SearchQueryResultEntity>)

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
