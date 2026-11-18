package io.github.alxiw.wearpagingmediator.data.mapper

import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoResponse

internal object RepoResponseMapper {
    fun toRepoEntity(response: RepoResponse): RepoEntity {
        return RepoEntity(
            id = response.id,
            name = response.fullName,
            description = response.description,
            url = response.url,
            stars = response.stars,
            forks = response.forks,
            language = response.language
        )
    }
}
