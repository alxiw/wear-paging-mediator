package io.github.alxiw.wearpagingmediator.data

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
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity
import io.github.alxiw.wearpagingmediator.data.mapper.RepoResponseMapper
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoResponse

@ExperimentalPagingApi
internal class ReposRemoteMediator(
    private val query: String,
    private val api: GitHubApi,
    private val database: GitHubDatabase
) : RemoteMediator<Int, RepoEntity>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, RepoEntity>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKey = getRemoteKeyClosestToCurrentPosition(state)
                remoteKey?.nextKey?.minus(1) ?: STARTING_PAGE_INDEX
            }
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

        // Remote keys cached before the search result limit was respected can point past the last
        // page GitHub serves, requesting it would fail with HTTP 422 instead of finishing the list.
        if (isBeyondSearchResultLimit(page, state.config.pageSize)) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        return runCatching {
            val loadSize = if (loadType == LoadType.REFRESH) state.config.initialLoadSize else state.config.pageSize
            val repos = api.searchRepos(
                query = query,
                page = page,
                pageSize = loadSize
            )
            val itemsLoaded = (page - 1) * state.config.pageSize + repos.items.size
            val nextPage = page + (loadSize / state.config.pageSize)
            val endOfPaginationReached = repos.items.isEmpty() ||
                itemsLoaded >= repos.total ||
                isBeyondSearchResultLimit(nextPage, state.config.pageSize)
            insertIntoDatabase(
                page = page,
                loadSize = loadSize,
                loadType = loadType,
                repoResponses = repos.items,
                endOfPaginationReached = endOfPaginationReached,
                state = state
            )
            MediatorResult.Success(endOfPaginationReached)
        }.getOrElse { throwable -> MediatorResult.Error(throwable) }
    }

    private suspend fun insertIntoDatabase(
        page: Int,
        loadSize: Int,
        loadType: LoadType,
        repoResponses: List<RepoResponse>,
        endOfPaginationReached: Boolean,
        state: PagingState<Int, RepoEntity>
    ) {
        val repos = repoResponses.map { RepoResponseMapper.toRepoEntity(it) }
        database.withTransaction {
            if (loadType == LoadType.REFRESH) {
                database.remoteKeysDao().clearRemoteKeys(query)
                database.reposDao().deleteResultsForQuery(query)
            }
            val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + (loadSize / state.config.pageSize)
            val keys = repos.map {
                RepoRemoteKeyEntity(repoId = it.id, query = query, prevKey = prevKey, nextKey = nextKey)
            }
            database.remoteKeysDao().insertAll(keys)
            database.reposDao().upsertAll(repos)

            val startPosition = (page - 1) * state.config.pageSize
            val searchResults = repos.mapIndexed { index, repo ->
                SearchQueryResultEntity(
                    query = query,
                    repoId = repo.id,
                    position = startPosition + index
                )
            }
            database.reposDao().insertSearchResults(searchResults)

            val topics = repoResponses.flatMap { it.topics }.distinct().map { TopicEntity(it) }
            database.reposDao().insertTopics(topics)

            val crossRefs = repoResponses.flatMap { response ->
                response.topics.map { topic -> RepoTopicCrossRef(response.id, topic) }
            }
            database.reposDao().insertRepoTopics(crossRefs)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, RepoEntity>): RepoRemoteKeyEntity? {
        return state.pages
            .lastOrNull { it.data.isNotEmpty() }
            ?.data?.lastOrNull()
            ?.let { repo -> database.remoteKeysDao().remoteKeysRepoId(repo.id, query) }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, RepoEntity>): RepoRemoteKeyEntity? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { repo -> database.remoteKeysDao().remoteKeysRepoId(repo.id, query) }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, RepoEntity>
    ): RepoRemoteKeyEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                database.remoteKeysDao().remoteKeysRepoId(repoId, query)
            }
        }
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
