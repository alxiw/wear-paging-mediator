package io.github.alxiw.wearpagingmediator.data

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity
import io.github.alxiw.wearpagingmediator.data.mapper.RepoResponseMapper
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoResponse
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery

/**
 * Keeps the cached results of a query in sync with GitHub: the Room PagingSource shows the rows of
 * a previous load as soon as the screen opens, while [load] refreshes them from the network. A page
 * is logged when GitHub is asked, so cached and fresh results can be told apart.
 *
 * A page always has [ReposRepository.NETWORK_PAGE_SIZE] results, both in the request and in Room: a
 * refresh replaces the slice around the anchor instead of clearing the cache, so the first result
 * stays at position 0 and a later cold start still opens at the head.
 *
 * [queryKey] is the canonical query, the only form the API and the cache see: the request and the
 * rows of one search cannot disagree on the case or the spacing of the query.
 */
@ExperimentalPagingApi
internal class ReposRemoteMediator(
    query: SearchQuery,
    private val api: GitHubApi,
    private val database: GitHubDatabase
) : RemoteMediator<Int, RepoEntity>() {

    /** The string the request of the page and the cached rows of the query are keyed by. */
    private val queryKey: String = query.value

    override suspend fun load(loadType: LoadType, state: PagingState<Int, RepoEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> refreshPage(state)
            LoadType.PREPEND -> {
                val remoteKey = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKey?.prevKey ?: return MediatorResult.Success(remoteKey != null)
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey ?: return MediatorResult.Success(remoteKeys != null)
                nextKey
            }
        }

        if (loadType == LoadType.REFRESH) {
            Log.d("HELLO", "$queryKey: page $page is loaded from GitHub")
        }

        val pageSize = state.config.pageSize
        // Remote keys cached before the search result limit was respected can point past the last
        // page GitHub serves, requesting it would fail with HTTP 422 instead of finishing the list.
        if (isBeyondSearchResultLimit(page, pageSize)) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        return runCatching {
            val repos = api.searchRepos(
                query = queryKey,
                page = page,
                pageSize = pageSize
            )
            val itemsLoaded = (page - 1) * pageSize + repos.items.size
            val nextPage = page + 1
            val endOfPaginationReached = repos.items.isEmpty() ||
                itemsLoaded >= repos.total ||
                isBeyondSearchResultLimit(nextPage, pageSize)
            insertIntoDatabase(
                page = page,
                pageSize = pageSize,
                repoResponses = repos.items,
                endOfPaginationReached = endOfPaginationReached
            )
            MediatorResult.Success(endOfPaginationReached)
        }.getOrElse { throwable -> MediatorResult.Error(throwable) }
    }

    /**
     * The first page after a refresh is the page that holds the anchor, not the page that happens
     * to follow its remote key. The key is taken from the same in-memory page that the anchor
     * belongs to; when that page is replaced while it is being refreshed, the next closest page
     * can belong to another search page and replay the wrong window.
     *
     * The anchor is found in Room instead: its stored position is the offset the PagingSource used,
     * so the same result keeps its place before and after the network page is merged into the
     * cache. A cache without the anchor (an old one, or a result that was dropped) starts from the
     * first page, which also gives the cache the head it needs for a cold start.
     */
    private suspend fun refreshPage(state: PagingState<Int, RepoEntity>): Int {
        val repoId = state.anchorPosition
            ?.let { position -> state.closestItemToPosition(position)?.id }
            ?: return STARTING_PAGE_INDEX
        val position = database.reposDao().positionOf(queryKey, repoId)
            ?: return STARTING_PAGE_INDEX
        return position / state.config.pageSize + STARTING_PAGE_INDEX
    }

    private suspend fun insertIntoDatabase(
        page: Int,
        pageSize: Int,
        repoResponses: List<RepoResponse>,
        endOfPaginationReached: Boolean
    ) {
        val repos = repoResponses.map { RepoResponseMapper.toRepoEntity(it) }
        val startPosition = (page - 1) * pageSize
        val endExclusive = startPosition + pageSize
        val searchResults = repos.mapIndexed { index, repo ->
            SearchQueryResultEntity(
                query = queryKey,
                repoId = repo.id,
                position = startPosition + index
            )
        }
        val repoIds = repos.map { it.id }

        database.withTransaction {
            // A page replaces exactly its own slice: the cached head and the following pages stay
            // where they are, so an anchor in the middle of the list is still valid after a
            // refresh. A shorter final page also drops the results that are no longer served.
            val endOfResults = if (endOfPaginationReached) startPosition + searchResults.size else null

            database.remoteKeysDao().deleteForPositions(queryKey, startPosition, endExclusive)
            if (endOfResults != null) {
                database.remoteKeysDao().deleteFromPosition(queryKey, endOfResults)
            }
            database.reposDao().deleteResultsForPositions(queryKey, startPosition, endExclusive)
            if (endOfResults != null) {
                database.reposDao().deleteResultsFromPosition(queryKey, endOfResults)
            }

            // A repo can move from one page to another when results are inserted or removed on
            // GitHub: drop its old result and key before it is written at its new position.
            if (repoIds.isNotEmpty()) {
                database.remoteKeysDao().deleteForRepoIds(queryKey, repoIds)
                database.reposDao().deleteResultsForQueryAndRepoIds(queryKey, repoIds)
            }

            database.reposDao().upsertAll(repos)

            val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + 1
            val keys = repos.map {
                RepoRemoteKeyEntity(
                    repoId = it.id,
                    query = queryKey,
                    prevKey = prevKey,
                    nextKey = nextKey
                )
            }
            database.remoteKeysDao().insertAll(keys)
            database.reposDao().insertSearchResults(searchResults)

            val crossRefs = repoResponses.flatMap { response ->
                response.topics.map { topic -> RepoTopicCrossRef(repoId = response.id, topic = topic) }
            }
            // the response holds the whole topic list of these repos, not only the new links
            if (repoIds.isNotEmpty()) database.reposDao().deleteRepoTopics(repoIds)
            database.reposDao().insertRepoTopics(crossRefs)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, RepoEntity>): RepoRemoteKeyEntity? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { repo -> database.remoteKeysDao().remoteKeysRepoId(repo.id, queryKey) }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, RepoEntity>): RepoRemoteKeyEntity? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { repo -> database.remoteKeysDao().remoteKeysRepoId(repo.id, queryKey) }
    }

    /**
     * GitHub search serves results only up to [MAX_SEARCH_RESULTS] offset and answers `HTTP 422`
     * ("Only the first 1000 search results are available") for deeper pages, even though
     * `total_count` of a query like `Android` is millions: pagination has to end there.
     */
    private fun isBeyondSearchResultLimit(page: Int, pageSize: Int): Boolean =
        (page - 1) * pageSize >= MAX_SEARCH_RESULTS

    companion object {
        const val STARTING_PAGE_INDEX = 1
        const val MAX_SEARCH_RESULTS = 1_000
    }
}
