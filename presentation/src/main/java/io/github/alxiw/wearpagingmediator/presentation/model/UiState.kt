package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY

data class UiState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    // the list has not been scrolled to the top for the current query yet
    val scrollToTopPending: Boolean = false
)
