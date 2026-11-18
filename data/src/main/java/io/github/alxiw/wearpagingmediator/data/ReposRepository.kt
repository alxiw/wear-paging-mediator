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
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                enablePlaceholders = false
            ),
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
    }
}
