package io.github.alxiw.wearpagingmediator.domain

import androidx.paging.PagingData
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import kotlinx.coroutines.flow.Flow

interface Repository {

    /**
     * The results of [query] as a paging stream. The query is canonical (see [SearchQuery]), and an
     * implementation normalizes a raw one itself as well, so callers cannot split the cache by
     * case or spacing.
     */
    fun getReposFlow(query: String): Flow<PagingData<Repo>>
}
