package io.github.alxiw.wearpagingmediator.presentation.model

import androidx.paging.LoadState

/** Content shown right below the search button, above the results of the current query. */
sealed interface ListNotice {
    data object Loading : ListNotice
    data object Empty : ListNotice
}

/** Content shown below the last item of the current query. */
sealed interface ListFooter {
    data object RefreshError : ListFooter
    data object Loading : ListFooter
    data object AppendError : ListFooter
    data object EndOfList : ListFooter
}

/**
 * Resolves what the list shows next to its items, so that the screen only renders the result
 * instead of repeating the conditions and a JVM test can guard them.
 *
 * The results look exactly the same no matter where they came from: cached results are not marked in
 * any way while they are refreshed in the background (the data layer logs whether the network was
 * asked instead), and a failure is reported after the last item, never above the first one, so the
 * head of the list is never taken by a state of the load.
 *
 * Both states are resolved from the [LoadState]s of the paging items:
 * * `refresh` never reports `endOfPaginationReached` for `RemoteMediator` backed data (it is always
 *   `false`), so "nothing found" is detected through the completed `append` state instead;
 * * every load state starts as `NotLoading.Incomplete`, so nothing is shown until a query has been
 *   loaded: the list must not flash "no results" on the first frame or right after a new search.
 */
object ListStatus {

    fun notice(refresh: LoadState, append: LoadState, itemCount: Int): ListNotice? = when {
        // the first page of the current query is on its way, there is nothing to keep on screen
        refresh is LoadState.Loading && itemCount == 0 -> ListNotice.Loading
        // the query has finished without a single item: the search has no results at all
        itemCount == 0 &&
            refresh is LoadState.NotLoading &&
            append.endOfPaginationReached -> ListNotice.Empty

        else -> null
    }

    fun footer(refresh: LoadState, append: LoadState, itemCount: Int): ListFooter? = when {
        // the query failed, cached results of the same query (if any) stay above the error
        refresh is LoadState.Error -> ListFooter.RefreshError
        // the next page failed, the items loaded so far stay usable
        append is LoadState.Error -> ListFooter.AppendError
        itemCount > 0 && append is LoadState.Loading -> ListFooter.Loading
        // GitHub search serves the first 1000 results only, the list ends there as well
        itemCount > 0 &&
            refresh is LoadState.NotLoading &&
            append.endOfPaginationReached -> ListFooter.EndOfList

        else -> null
    }
}
