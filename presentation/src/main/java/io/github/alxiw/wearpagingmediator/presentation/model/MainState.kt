package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY

/**
 * The state of the screen: the query the results belong to and whether the list still has to be
 * scrolled back to the top for it.
 *
 * [query] is canonical (see `SearchQuery`): it is what the button shows and the key the paging
 * flow uses, so a search typed in another case is the same search here as well.
 *
 * The results themselves are not a part of the state: they are a `Flow<PagingData>` whose load
 * states are owned by the paging items of the list, see `MainViewModel.pagingDataFlow`.
 */
data class MainState(
    val query: String = DEFAULT_QUERY,
    val lastQueryScrolled: String = DEFAULT_QUERY,
    // the list has not been scrolled to the top for the current query yet
    val scrollToTopPending: Boolean = false
)
