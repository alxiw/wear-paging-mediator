package io.github.alxiw.wearpagingmediator.domain

import androidx.paging.PagingData
import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface Repository {

    /**
     * The results of [query] as a paging stream. The query is canonically built, so the request and
     * the cache of an implementation cannot be split by the case or the spacing of one search.
     *
     * An implementation registers the query as opened here: a window that is shown again without a
     * new stream (the screen keeps some of them) does not move up among the recent queries.
     */
    fun getReposFlow(query: SearchQuery): Flow<PagingData<Repo>>

    /** The topics of [repoId] as they are cached, the whole list of them. */
    suspend fun getRepoTopics(repoId: Long): List<String>

    /**
     * The queries that were opened most recently, the most recently used one first. At most
     * [RecentQuery.MAX_CACHED_QUERIES] of them are kept, the same number the dialog of the recent
     * queries offers.
     */
    suspend fun getRecentQueries(): List<RecentQuery>
}
