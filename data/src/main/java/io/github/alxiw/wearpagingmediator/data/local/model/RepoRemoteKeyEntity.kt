package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * The page keys of the repository [repoId] in the results of [query], which is canonical (see
 * `SearchQuery`): the same search typed in another case shares the keys.
 */
@Entity(
    tableName = "repo_remote_keys",
    primaryKeys = ["repo_id", "query"]
)
data class RepoRemoteKeyEntity(
    @ColumnInfo(name = "repo_id")
    val repoId: Long,
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "prev_key")
    val prevKey: Int?,
    @ColumnInfo(name = "next_key")
    val nextKey: Int?
)
