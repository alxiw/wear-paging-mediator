package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.Entity

/**
 * The page keys of the repository [repoId] in the results of [query], which is canonical (see
 * `SearchQuery`): the same search typed in another case shares the keys.
 */
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
