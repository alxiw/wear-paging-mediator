package io.github.alxiw.wearpagingmediator.data.remote.api

import io.github.alxiw.wearpagingmediator.data.remote.model.RepoSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

internal interface GitHubApi {

    @GET("search/repositories")
    suspend fun searchRepos(
        /**
         * The query with words separated by spaces: Retrofit percent-encodes the value, so
         * `android kotlin` goes on the wire as `q=android%20kotlin` and GitHub reads it as two
         * words that both have to match.
         */
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
