package io.github.alxiw.wearpagingmediator.presentation.testing

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alxiw.wearpagingmediator.domain.Repository
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import kotlinx.coroutines.flow.Flow

/**
 * A repository that serves the results of a query through a real [Pager], so the screen is tested
 * against the real load states of paging instead of hand made ones.
 *
 * [load] decides what a page does, which lets a test hold a page on its way (the screen keeps
 * waiting for it) or fail it.
 */
internal class FakeRepository : Repository {

    /** The queries the results were asked for, in the order they were asked for. */
    val queries = mutableListOf<String>()

    /** The pages that were requested, in the order they were requested. */
    val loads = mutableListOf<PagingSource.LoadParams<Int>>()

    /** How many pages were served, i.e. how many times the screen was given results to show. */
    var pagesServed = 0
        private set

    /** What a page of the given query does, a full page of [REPOS] by default. */
    var load: suspend (query: String, params: PagingSource.LoadParams<Int>) -> PagingSource.LoadResult<Int, Repo> =
        { _, _ -> page(REPOS) }

    override fun getReposFlow(query: String): Flow<PagingData<Repo>> {
        queries += query
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { Source(query) }
        ).flow
    }

    private inner class Source(private val query: String) : PagingSource<Int, Repo>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
            loads += params
            return load(query, params).also { pagesServed++ }
        }

        override fun getRefreshKey(state: PagingState<Int, Repo>): Int? = null
    }
}
