package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.Repo
import io.github.alxiw.wearpagingmediator.presentation.util.UiText

/**
 * An item of the list of results. The list identifies an item by its [stableKey] and reuses the
 * composition of the items of the same [contentType], so both are a part of the model instead of
 * a `when` in the screen.
 */
sealed class ListModel {

    /** The key that identifies the item across the pages of the same query. */
    abstract val stableKey: Any

    /** The kind of the item: the list reuses the composition of the items of the same kind. */
    abstract val contentType: String

    data class RepoItem(val repo: Repo) : ListModel() {
        override val stableKey: Any get() = repo.id
        override val contentType: String get() = REPO
    }

    data class SeparatorItem(val description: UiText) : ListModel() {
        override val stableKey: Any get() = description.stableKey
        override val contentType: String get() = SEPARATOR
    }

    private companion object {
        const val REPO = "repo"
        const val SEPARATOR = "separator"
    }
}
