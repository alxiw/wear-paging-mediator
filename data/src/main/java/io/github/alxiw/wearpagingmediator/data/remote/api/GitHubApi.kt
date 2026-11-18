package io.github.alxiw.wearpagingmediator.data.remote.api

import io.github.alxiw.wearpagingmediator.data.remote.model.RepoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

internal interface GitHubApi {

    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") pageSize: Int,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
    ): RepoSearchResponse

    companion object {
        const val BASE_URL = "https://api.github.com/"
    }
}
