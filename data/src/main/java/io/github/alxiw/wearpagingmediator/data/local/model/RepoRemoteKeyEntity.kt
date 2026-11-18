package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.Entity

@Entity(
    tableName = "repo_remote_keys",
    primaryKeys = ["repoId", "query"]
)
data class RepoRemoteKeyEntity(
    val repoId: Long,
    val query: String,
    val prevKey: Int?,
    val nextKey: Int?
)
