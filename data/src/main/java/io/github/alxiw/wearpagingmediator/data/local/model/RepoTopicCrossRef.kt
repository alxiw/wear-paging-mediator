package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/** One topic of the repository [repoId]. */
@Entity(
    tableName = "repo_topic_cross_ref",
    primaryKeys = ["repo_id", "topic"],
    indices = [Index(value = ["topic"])]
)
data class RepoTopicCrossRef(
    @ColumnInfo(name = "repo_id")
    val repoId: Long,
    @ColumnInfo(name = "topic")
    val topic: String
)
