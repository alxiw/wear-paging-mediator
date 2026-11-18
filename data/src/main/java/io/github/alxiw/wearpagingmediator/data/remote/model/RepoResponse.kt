package io.github.alxiw.wearpagingmediator.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity

@JsonClass(generateAdapter = true)
data class RepoResponse(
    @Json(name = "id")
    val id: Long,
    @Json(name = "name")
    val name: String,
    @Json(name = "full_name")
    val fullName: String,
    @Json(name = "description")
    val description: String?,
    @Json(name = "html_url")
    val url: String,
    @Json(name = "stargazers_count")
    val stars: Int,
    @Json(name = "forks_count")
    val forks: Int,
    @Json(name = "language")
    val language: String?,
    @Json(name = "topics")
    val topics: List<String> = emptyList()
)

fun RepoResponse.toRepoEntity() = RepoEntity(
    id = id,
    name = fullName,
    description = description,
    url = url,
    stars = stars,
    forks = forks,
    language = language
)
