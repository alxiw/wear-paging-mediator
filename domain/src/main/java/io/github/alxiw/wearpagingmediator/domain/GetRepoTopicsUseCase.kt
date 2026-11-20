package io.github.alxiw.wearpagingmediator.domain

import javax.inject.Inject

/** Reads the topics of one repository when they are needed, not with every page of the search. */
class GetRepoTopicsUseCase @Inject constructor(
    private val repository: Repository
) {
    suspend operator fun invoke(repoId: Long): List<String> = repository.getRepoTopics(repoId)
}
