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

    @Query("SELECT * FROM repo_remote_keys WHERE repoId = :repoId AND `query` = :query")
    suspend fun remoteKeysRepoId(repoId: Long, query: String): RepoRemoteKeyEntity?

    @Query("DELETE FROM repo_remote_keys WHERE `query` = :query")
    suspend fun clearRemoteKeys(query: String)
}
