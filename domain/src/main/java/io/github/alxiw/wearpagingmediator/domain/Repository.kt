package io.github.alxiw.wearpagingmediator.domain

import androidx.paging.PagingData
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import kotlinx.coroutines.flow.Flow

interface Repository {

    fun getReposFlow(query: String): Flow<PagingData<Repo>>
}
