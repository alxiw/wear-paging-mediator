package io.github.alxiw.wearpagingmediator.presentation.model

sealed class UiAction {
    data class Search(val query: String) : UiAction()
    data class Scroll(val query: String) : UiAction()
}
