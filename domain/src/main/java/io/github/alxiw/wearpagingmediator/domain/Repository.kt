package io.github.alxiw.wearpagingmediator.domain

import androidx.paging.PagingData
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow

interface Repository {

    /**
     * The results of [query] as a paging stream. The query is canonical by its type ([io.github.alxiw.wearpagingmediator.domain.model.SearchQuery]
     * can be built only by [io.github.alxiw.wearpagingmediator.domain.model.SearchQuery.of]), so the network request and the cache of an
     * implementation cannot be split by the case or the spacing of one search.
     */
    fun getReposFlow(query: SearchQuery): Flow<PagingData<Repo>>
}
