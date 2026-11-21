package io.github.alxiw.wearpagingmediator.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.mapper.RepoEntityMapper
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.domain.Repository
import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

internal class ReposRepository @Inject constructor(
    private val api: GitHubApi,
    private val database: GitHubDatabase,
    private val recentQueries: RecentQueriesCache
) : Repository {

    /**
     * The query is registered as opened before the results are paged, so the cache evicts some
     * other query and cannot invalidate the paging source of the one it is preparing.
     */
    override fun getReposFlow(query: SearchQuery): Flow<PagingData<Repo>> = flow {
        try {
            recentQueries.touch(query.value)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // a failure to maintain the cache must not fail the search itself
            Log.w("HELLO", "${query.value}: cache maintenance failed", error)
        }
        emitAll(pagerFlow(query))
    }

    @OptIn(ExperimentalPagingApi::class)
    private fun pagerFlow(query: SearchQuery): Flow<PagingData<Repo>> {
        // The query is canonical by its type, so the mediator and the Room source key on the same
        // value: Retrofit percent-encodes it for @Query, the database stores it as a readable key.
        return Pager(
            config = pagingConfig,
            remoteMediator = ReposRemoteMediator(
                query = query,
                api = api,
                database = database
            ),
            pagingSourceFactory = {
                database.reposDao().getReposForQuery(query.value)
            }
        ).flow.map { pagingData -> pagingData.map { RepoEntityMapper.toRepo(it) } }
    }

    override suspend fun getRepoTopics(repoId: Long): List<String> =
        database.reposDao().topicsOf(repoId)

    /**
     * The cache keeps more queries than the screen caches in memory, so the dialog can always
     * show the full number of the most recently used ones.
     */
    override suspend fun getRecentQueries(): List<RecentQuery> =
        database.searchQueriesDao()
            .recentQueries(RecentQuery.MAX_CACHED_QUERIES)
            .map { entity ->
                RecentQuery(
                    query = SearchQuery.of(entity.query),
                    accessedAt = Instant.ofEpochMilli(entity.accessedAt)
                )
            }

    companion object {
        const val NETWORK_PAGE_SIZE = 30

        /**
         * Bounds the window of pages kept in memory: everything is already persisted in Room, so
         * pages dropped from the window are re-read on scroll back. The size covers the initial
         * load of two pages plus a few appended ones, and it is bigger than twice the initial load,
         * so a dropped page can never break scrolling in both directions.
         *
         * The initial load is smaller than the default one of three pages: the first results of a
         * new query are shown as soon as they are parsed, which matters on a watch where the
         * payloads are large. The prefetch distance stays below one page, so restoring a scroll
         * position does not append in the background.
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
