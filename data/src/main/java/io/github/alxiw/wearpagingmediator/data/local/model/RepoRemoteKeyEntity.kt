package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "repo_remote_keys")
data class RepoRemoteKeyEntity(
    @PrimaryKey
    val repoId: Long,
    val prevKey: Int?,
    val nextKey: Int?
)
