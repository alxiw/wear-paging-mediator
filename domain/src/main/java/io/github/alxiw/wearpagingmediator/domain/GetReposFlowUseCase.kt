package io.github.alxiw.wearpagingmediator.domain

import androidx.paging.PagingData
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReposFlowUseCase @Inject constructor(
    private val repository: Repository
) {
    operator fun invoke(query: SearchQuery): Flow<PagingData<Repo>> {
        return repository.getReposFlow(query)
    }
}
