package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A query whose results are cached: [accessedAt] is the moment its Pager was created, not the
 * moment GitHub answered, and the least recently used rows are the ones the cache evicts first.
 */
@Entity(tableName = "search_queries")
data class SearchQueryEntity(
    @PrimaryKey
    @ColumnInfo(name = "query")
    val query: String,
    @ColumnInfo(name = "accessed_at")
    val accessedAt: Long
)
