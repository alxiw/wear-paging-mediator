package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
