package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY

/**
 * The state of the screen: the query the results belong to and whether the list still has to be
 * scrolled back to the top for it.
 *
 * The results themselves are not a part of the state: they are a `Flow<PagingData>` whose load
 * states are owned by the paging items of the list, see `MainViewModel.pagingDataFlow`.
 */
data class MainState(
    val query: SearchQuery = DEFAULT_QUERY,
    val lastQueryScrolled: SearchQuery = DEFAULT_QUERY,
    // the list has not been scrolled to the top for the current query yet
    val scrollToTopPending: Boolean = false,
    /** The topics dialog of the tapped result, `null` while the dialog is closed. */
    val topicsDialog: TopicsDialogState? = null
)
