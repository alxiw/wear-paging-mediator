package io.github.alxiw.wearpagingmediator.domain

import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import javax.inject.Inject

/** Reads the most recently opened queries when the dialog with them is opened. */
class GetRecentQueriesUseCase @Inject constructor(
    private val repository: Repository
) {
    suspend operator fun invoke(): List<RecentQuery> = repository.getRecentQueries()
}
