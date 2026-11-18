package io.github.alxiw.wearpagingmediator.data.mapper

import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.domain.model.Repo

internal object RepoEntityMapper {
    fun toRepo(entity: RepoEntity): Repo {
        return Repo(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            url = entity.url,
            stars = entity.stars,
            forks = entity.forks,
            language = entity.language
        )
    }
}
