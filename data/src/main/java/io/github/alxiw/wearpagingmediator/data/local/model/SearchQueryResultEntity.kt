package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * One result of a search: the repository [repoId] at [position] of the result list of [query].
 *
 * [query] is canonical (see `SearchQuery`), so a query typed in another case or with other
 * whitespace finds the same cached results.
 */
@Entity(
    tableName = "search_query_results",
    primaryKeys = ["query", "repo_id"],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            parentColumns = ["id"],
            childColumns = ["repo_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("repo_id")]
)
data class SearchQueryResultEntity(
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "repo_id")
    val repoId: Long,
    @ColumnInfo(name = "position")
    val position: Int
)
