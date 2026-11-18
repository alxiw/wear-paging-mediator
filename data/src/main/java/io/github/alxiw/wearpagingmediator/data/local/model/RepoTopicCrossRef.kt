package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "repo_topic_cross_ref",
    primaryKeys = ["id", "topic"],
    indices = [Index(value = ["topic"])]
)
data class RepoTopicCrossRef(
    val id: Long,
    val topic: String
)
