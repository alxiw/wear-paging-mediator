package io.github.alxiw.wearpagingmediator.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class RepoSearchResponse(
    @Json(name = "total_count")
    val total: Int = 0,
    @Json(name = "items")
    val items: List<RepoResponse> = emptyList(),
    val nextPage: Int? = null
)
