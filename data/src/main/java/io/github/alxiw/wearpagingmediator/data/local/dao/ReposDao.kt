package io.github.alxiw.wearpagingmediator.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity

@Dao
internal interface ReposDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(repos: List<RepoEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepoTopics(crossRefs: List<RepoTopicCrossRef>)

    @Query("""
        SELECT DISTINCT repos.* FROM repos 
        LEFT JOIN repo_topic_cross_ref ON repos.id = repo_topic_cross_ref.id 
        WHERE repos.name LIKE :query 
        OR repos.description LIKE :query 
        OR repo_topic_cross_ref.topic LIKE :query 
        ORDER BY stars DESC, name ASC
    """)
    fun getRepos(query: String): PagingSource<Int, RepoEntity>

    @Query("DELETE FROM repos")
    suspend fun deleteAll()

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()

    @Query("DELETE FROM repo_topic_cross_ref")
    suspend fun deleteAllRepoTopics()
}
