package io.github.alxiw.wearpagingmediator.data.local.model

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
    primaryKeys = ["query", "repoId"],
    foreignKeys = [
        ForeignKey(
            entity = RepoEntity::class,
            parentColumns = ["id"],
            childColumns = ["repoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("repoId")]
)
data class SearchQueryResultEntity(
    val query: String,
    val repoId: Long,
    val position: Int
)
