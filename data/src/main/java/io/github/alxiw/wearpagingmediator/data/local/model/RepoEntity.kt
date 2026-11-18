package io.github.alxiw.wearpagingmediator.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.alxiw.wearpagingmediator.domain.model.Repo

@Entity(tableName = "repos")
data class RepoEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "url")
    val url: String,
    @ColumnInfo(name = "stars")
    val stars: Int,
    @ColumnInfo(name = "forks")
    val forks: Int,
    @ColumnInfo(name = "language")
    val language: String?
)

fun RepoEntity.toRepo() = Repo(
    id = id,
    name = name,
    description = description,
    url = url,
    stars = stars,
    forks = forks,
    language = language
)
