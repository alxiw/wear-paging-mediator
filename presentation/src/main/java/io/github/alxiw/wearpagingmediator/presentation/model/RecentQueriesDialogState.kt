package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery

/**
 * The dialog with the most recently used queries. Every entry keeps the moment its query was
 * used, so a search can be recognized by when it was run and not only by its text.
 */
sealed interface RecentQueriesDialogState {

    /** The dialog is open, the queries are being read. */
    data object Loading : RecentQueriesDialogState

    /** The dialog shows [queries], the most recently used one first, possibly none. */
    data class Loaded(val queries: List<RecentQuery>) : RecentQueriesDialogState

    /** The queries could not be read. */
    data object Failed : RecentQueriesDialogState
}
