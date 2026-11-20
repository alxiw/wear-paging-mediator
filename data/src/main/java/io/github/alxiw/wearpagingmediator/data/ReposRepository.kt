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
import io.github.alxiw.wearpagingmediator.domain.SearchQuery
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
        // GitHub is case insensitive and reads words separated by a space as one query, so the
        // canonical form is what the network and Room have to key on. Retrofit percent-encodes
        // the value of @Query, the database stores the readable query with its spaces.
        val searchQuery = SearchQuery.normalize(query)
        return Pager(
            config = pagingConfig,
            remoteMediator = ReposRemoteMediator(
                query = searchQuery,
                api = api,
                database = database
            ),
            pagingSourceFactory = {
                database.reposDao().getReposForQuery(searchQuery)
            }
        ).flow.map { pagingData -> pagingData.map { RepoEntityMapper.toRepo(it) } }
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 30

        /**
         * Bounds the window of pages kept in memory: everything is already persisted in Room, so
         * pages dropped from the window are re-read on scroll back instead of being kept forever.
         * The size covers the initial load (2 pages) plus a few appended ones, and it is bigger
         * than twice the initial load so a dropped page can never break scrolling in both
         * directions.
         *
         * The initial load is smaller than the initial load size of the default config (3 pages):
         * the first page of a new query is shown as soon as it is parsed, which matters on a watch
         * where GitHub search answers with large repository payloads. The prefetch distance stays
         * below one page, so restoring a scroll position does not append in the background.
         */
        internal val pagingConfig = PagingConfig(
            pageSize = NETWORK_PAGE_SIZE,
            initialLoadSize = NETWORK_PAGE_SIZE * 2,
            maxSize = NETWORK_PAGE_SIZE * 6,
            prefetchDistance = NETWORK_PAGE_SIZE / 3,
            enablePlaceholders = false
        )
    }
}
