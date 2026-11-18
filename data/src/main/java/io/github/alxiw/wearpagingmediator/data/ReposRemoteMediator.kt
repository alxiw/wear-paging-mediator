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
import io.github.alxiw.wearpagingmediator.data.local.model.TopicEntity
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoResponse
import io.github.alxiw.wearpagingmediator.data.remote.model.toRepoEntity

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

        return runCatching {
            val repos = api.searchRepos(
                query = query,
                page = page,
                pageSize = state.config.pageSize
            )
            val itemsLoaded = (page - 1) * state.config.pageSize + repos.items.size
            val endOfPaginationReached = repos.items.isEmpty() || itemsLoaded >= repos.total
            insertIntoDatabase(
                page = page,
                loadType = loadType,
                repoResponses = repos.items,
                endOfPaginationReached = endOfPaginationReached
            )
            MediatorResult.Success(endOfPaginationReached)
        }.getOrElse { throwable -> MediatorResult.Error(throwable) }
    }

    private suspend fun insertIntoDatabase(
        page: Int,
        loadType: LoadType,
        repoResponses: List<RepoResponse>,
        endOfPaginationReached: Boolean
    ) {
        val repos = repoResponses.map { it.toRepoEntity() }
        database.withTransaction {
            if (loadType == LoadType.REFRESH) {
                database.remoteKeysDao().clearRemoteKeys()
                database.reposDao().deleteAll()
                database.reposDao().deleteAllTopics()
                database.reposDao().deleteAllRepoTopics()
            }
            val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
            val nextKey = if (endOfPaginationReached) null else page + 1
            val keys = repos.map {
                RepoRemoteKeyEntity(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
            }
            database.remoteKeysDao().insertAll(keys)
            database.reposDao().insertAll(repos)

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
            ?.let { repo -> database.remoteKeysDao().remoteKeysRepoId(repo.id) }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, RepoEntity>): RepoRemoteKeyEntity? {
        return state.pages
            .firstOrNull { it.data.isNotEmpty() }
            ?.data?.firstOrNull()
            ?.let { repo -> database.remoteKeysDao().remoteKeysRepoId(repo.id) }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, RepoEntity>
    ): RepoRemoteKeyEntity? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let {
                repoId -> database.remoteKeysDao().remoteKeysRepoId(repoId)
            }
        }
    }

    companion object {
        const val STARTING_PAGE_INDEX = 1
        const val NETWORK_PAGE_SIZE = 30
    }
}
