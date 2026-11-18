package io.github.alxiw.wearpagingmediator.domain.model

data class Repo(
    val id: Long,
    val name: String,
    val description: String?,
    val url: String,
    val stars: Int,
    val forks: Int,
    val language: String?
)
