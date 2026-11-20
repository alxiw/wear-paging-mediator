package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A query the user opened: the cache keeps the results of the most recently opened queries only,
 * so [accessedAt] is the moment the query was opened, not the moment GitHub answered.
 */
@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @PrimaryKey
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "accessed_at")
    val accessedAt: Long
)
