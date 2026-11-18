package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey
    val topic: String
)
