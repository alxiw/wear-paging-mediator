package io.github.alxiw.wearpagingmediator.presentation.model

import androidx.paging.LoadState
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards what the list shows next to the results, see [ListStatus].
 *
 * The screen must never swap the whole content (search button included) for a spinner, and the
 * results must look the same whether they come from the cache or from the network: only states the
 * user has to react to are reported, and they are reported after the last item.
 */
class ListStatusTest {

    @Test
    fun nothingIsShownBeforeTheFirstLoadStateArrives() {
        assertNull(ListStatus.notice(INCOMPLETE, INCOMPLETE, itemCount = 0))
        assertNull(ListStatus.footer(INCOMPLETE, INCOMPLETE, itemCount = 0))
    }

    @Test
    fun loadingIsShownWhileTheQueryHasNothingToDisplay() {
        assertEquals(
            ListNotice.Loading,
            ListStatus.notice(LoadState.Loading, INCOMPLETE, itemCount = 0)
        )
        assertNull(ListStatus.footer(LoadState.Loading, INCOMPLETE, itemCount = 0))
    }

    @Test
    fun cachedResultsAreNotMarkedWhileTheyAreRefreshed() {
        // a background refresh is not reported at all: the list has to look exactly like the one
        // that was just loaded from the network
        assertNull(ListStatus.notice(LoadState.Loading, INCOMPLETE, itemCount = PAGE_SIZE))
        assertNull(ListStatus.footer(LoadState.Loading, INCOMPLETE, itemCount = PAGE_SIZE))
    }

    @Test
    fun failedRefreshIsReportedAfterTheLastItem() {
        val error = LoadState.Error(IOException("offline"))

        // nothing is reported above the results, the cached ones stay exactly as they were
        assertNull(ListStatus.notice(error, INCOMPLETE, itemCount = PAGE_SIZE))
        assertEquals(
            ListFooter.RefreshError,
            ListStatus.footer(error, INCOMPLETE, itemCount = PAGE_SIZE)
        )
        // a search without cached results reports the failure in the same place
        assertNull(ListStatus.notice(error, INCOMPLETE, itemCount = 0))
        assertEquals(
            ListFooter.RefreshError,
            ListStatus.footer(error, INCOMPLETE, itemCount = 0)
        )
    }

    @Test
    fun failedRefreshHidesTheEndOfTheList() {
        val error = LoadState.Error(IOException("offline"))

        assertEquals(
            ListFooter.RefreshError,
            ListStatus.footer(error, COMPLETE, itemCount = PAGE_SIZE)
        )
    }

    @Test
    fun loadingQueryIsNotReportedAsEmpty() {
        // the append state of the previous generation is complete, the new query is still loading:
        // it has to keep showing progress instead of "no results"
        assertEquals(ListNotice.Loading, ListStatus.notice(LoadState.Loading, COMPLETE, itemCount = 0))
    }

    @Test
    fun searchWithoutResultsIsReported() {
        assertEquals(ListNotice.Empty, ListStatus.notice(INCOMPLETE, COMPLETE, itemCount = 0))
        // an empty search is not a list with an end, there is no list at all
        assertNull(ListStatus.footer(INCOMPLETE, COMPLETE, itemCount = 0))
    }

    @Test
    fun appendingIsReportedInTheFooter() {
        assertEquals(
            ListFooter.Loading,
            ListStatus.footer(INCOMPLETE, LoadState.Loading, itemCount = PAGE_SIZE)
        )
        assertNull(ListStatus.notice(INCOMPLETE, LoadState.Loading, itemCount = PAGE_SIZE))
    }

    @Test
    fun failedAppendIsReportedInTheFooterWithRetry() {
        val error = LoadState.Error(IOException("offline"))

        assertEquals(
            ListFooter.AppendError,
            ListStatus.footer(INCOMPLETE, error, itemCount = PAGE_SIZE)
        )
        assertNull(ListStatus.notice(INCOMPLETE, error, itemCount = PAGE_SIZE))
    }

    @Test
    fun lastPageOfResultsEndsTheList() {
        assertEquals(
            ListFooter.EndOfList,
            ListStatus.footer(INCOMPLETE, COMPLETE, itemCount = PAGE_SIZE)
        )
        // the GitHub search results limit ends the list in exactly the same way
        assertEquals(
            ListFooter.EndOfList,
            ListStatus.footer(INCOMPLETE, COMPLETE, itemCount = MAX_SEARCH_RESULTS)
        )
    }

    @Test
    fun endOfListIsNotReportedWhileTheQueryIsStillRefreshing() {
        assertNull(ListStatus.footer(LoadState.Loading, COMPLETE, itemCount = PAGE_SIZE))
    }

    private companion object {
        val INCOMPLETE = LoadState.NotLoading(endOfPaginationReached = false)
        val COMPLETE = LoadState.NotLoading(endOfPaginationReached = true)
        const val PAGE_SIZE = 30
        const val MAX_SEARCH_RESULTS = 1_000
    }
}
