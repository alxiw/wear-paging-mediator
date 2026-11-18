package io.github.alxiw.wearpagingmediator.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.remote.model.toRepoEntity

internal class ReposPagingSource(
    private val api: GitHubApi,
    private val query: String
) : PagingSource<Int, RepoEntity>() {

    @OptIn(ExperimentalPagingApi::class)
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RepoEntity> {
        val position = params.key ?: ReposRemoteMediator.STARTING_PAGE_INDEX
        return runCatching {
            val response = api.searchRepos(query, position, params.loadSize)
            val repos = response.items.map { it.toRepoEntity() }
            val itemsLoaded = (position - 1) * ReposRemoteMediator.NETWORK_PAGE_SIZE + repos.size
            val nextKey = if (repos.isEmpty() || itemsLoaded >= response.total) {
                null
            } else {
                position + (params.loadSize / ReposRemoteMediator.NETWORK_PAGE_SIZE)
            }
            LoadResult.Page(
                data = repos,
                prevKey = if (position == ReposRemoteMediator.STARTING_PAGE_INDEX) null else position - 1,
                nextKey = nextKey
            )
        }.getOrElse { throwable -> LoadResult.Error(throwable) }
    }

    override fun getRefreshKey(state: PagingState<Int, RepoEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
