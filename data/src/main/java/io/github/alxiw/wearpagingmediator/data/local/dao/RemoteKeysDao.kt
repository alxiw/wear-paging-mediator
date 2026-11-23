package io.github.alxiw.wearpagingmediator.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<RepoRemoteKeyEntity>)

    @Query("SELECT * FROM repo_remote_keys WHERE repo_id = :repoId AND `query` = :query")
    suspend fun remoteKeysRepoId(repoId: Long, query: String): RepoRemoteKeyEntity?

    @Query("DELETE FROM repo_remote_keys WHERE `query` IN (:queries)")
    suspend fun deleteForQueries(queries: List<String>)

    /** Deletes the keys that belong to the results of one page. */
    @Query("""
        DELETE FROM repo_remote_keys
        WHERE `query` = :query AND repo_id IN (
            SELECT repo_id FROM search_query_results
            WHERE `query` = :query AND position >= :start AND position < :endExclusive
        )
    """)
    suspend fun deleteForPositions(query: String, start: Int, endExclusive: Int)

    /** Deletes the keys of stale results after a smaller final page. */
    @Query("""
        DELETE FROM repo_remote_keys
        WHERE `query` = :query AND repo_id IN (
            SELECT repo_id FROM search_query_results
            WHERE `query` = :query AND position >= :position
        )
    """)
    suspend fun deleteFromPosition(query: String, position: Int)

    /** Drops the key of a repository that is inserted at another position. */
    @Query("DELETE FROM repo_remote_keys WHERE `query` = :query AND repo_id IN (:repoIds)")
    suspend fun deleteForRepoIds(query: String, repoIds: List<Long>)
}
