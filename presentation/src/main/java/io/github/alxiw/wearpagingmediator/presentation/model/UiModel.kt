package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.Repo

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}
