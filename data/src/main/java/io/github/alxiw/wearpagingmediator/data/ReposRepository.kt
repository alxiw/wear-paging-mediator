package io.github.alxiw.wearpagingmediator.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.mapper.RepoEntityMapper
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.domain.Repository
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ReposRepository @Inject constructor(
    private val api: GitHubApi,
    private val database: GitHubDatabase
) : Repository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getReposFlow(query: String): Flow<PagingData<Repo>> {
        val apiQuery = query.trim().replace(' ', '_')
        return Pager(
            config = pagingConfig,
            remoteMediator = ReposRemoteMediator(
                query = apiQuery,
                api = api,
                database = database
            ),
            pagingSourceFactory = {
                // "%${query.trim()}%".replace(' ', '%') for searchAllRepos
                database.reposDao().getReposForQuery(apiQuery)
            }
        ).flow.map { pagingData -> pagingData.map { RepoEntityMapper.toRepo(it) } }
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30

        /**
         * Bounds the window of pages kept in memory: everything is already persisted in Room, so
         * pages dropped from the window are re-read on scroll back instead of being kept forever.
         * The size covers the initial load (3 pages) plus a few appended ones, and it is bigger
         * than the initial load so a dropped page can never break scrolling in both directions.
         */
        internal val pagingConfig = PagingConfig(
            pageSize = NETWORK_PAGE_SIZE,
            maxSize = NETWORK_PAGE_SIZE * 6,
            enablePlaceholders = false
        )
    }
}
