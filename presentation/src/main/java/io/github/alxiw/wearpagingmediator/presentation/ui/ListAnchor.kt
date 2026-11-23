package io.github.alxiw.wearpagingmediator.presentation.ui

import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import io.github.alxiw.wearpagingmediator.presentation.model.ListModel
import io.github.alxiw.wearpagingmediator.presentation.model.ListStatus
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * The results the list showed around its anchor before a refresh. The keys are in the order of
 * their distance to the centre of the screen, so the closest result that still exists after the
 * refresh restores the context even when the anchored repository itself was moved or dropped.
 *
 * Only results and separators are kept: the search button and the footer of the list are not a
 * stable place for a user to return to, and their keys are not the keys of a page.
 */
internal data class ListAnchor(
    private val keys: List<Any>,
    val scrollOffset: Int
) {

    /**
     * The index of the first key that is still in [repos], shifted by the items above the results
     * (the search button and, when there are no results yet, the notice).
     */
    fun indexIn(repos: LazyPagingItems<ListModel>, headerItemCount: Int): Int? {
        val items = repos.itemSnapshotList.items
        keys.forEach { key ->
            val index = items.indexOfFirst { it.stableKey == key }
            if (index >= 0) return index + headerItemCount
        }
        return null
    }
}

/**
 * Reads the results around the centre of the viewport. A refresh of the same query keeps the old
 * data on screen while the next window is loaded, but the list can lose the anchor key when the
 * new window starts before it: this is the position that has to be restored afterwards.
 */
internal fun TransformingLazyColumnState.captureListAnchor(
    repos: LazyPagingItems<ListModel>
): ListAnchor? {
    // the head has nowhere to return to, and moving the first result to the centre would hide
    // the search button that the user expects at the top
    if (anchorItemIndex <= FIRST_RESULT_INDEX) return null

    val items = repos.itemSnapshotList.items
    if (items.isEmpty()) return null

    val stableKeys = items.mapTo(HashSet(items.size)) { it.stableKey }
    val visible = layoutInfo.visibleItems.filter { it.key in stableKeys }
    if (visible.isEmpty()) return null

    val viewportCentre = layoutInfo.viewportSize.height / 2
    val anchorKey = visible.firstOrNull { it.index == anchorItemIndex }?.key
    val keys = (listOfNotNull(anchorKey) + visible
        .sortedBy { item -> abs(item.offset + item.transformedHeight / 2 - viewportCentre) }
        .map { it.key })
        .distinct()

    return ListAnchor(keys = keys, scrollOffset = anchorItemScrollOffset)
}

/** The number of items the screen puts above the results of the query. */
internal fun LazyPagingItems<ListModel>.headerItemCount(): Int {
    val state = loadState
    val notice = ListStatus.notice(state.refresh, state.append, itemCount)
    return 1 + if (notice == null) 0 else 1
}

/**
 * Waits for the refresh that was just requested. The collection is started before `refresh()` is
 * called, so even a cache that answers within one frame cannot be missed. A refresh that never
 * starts or never finishes returns `false` instead of leaving the caller waiting forever.
 */
internal suspend fun LazyPagingItems<ListModel>.awaitRefreshFinished(): Boolean {
    val states = snapshotFlow { loadState.refresh }
    return withTimeoutOrNull(REFRESH_TIMEOUT_MILLIS.milliseconds) {
        states.first { it is LoadState.Loading }
        states.first { it !is LoadState.Loading }
        true
    } ?: false
}

/** The index of the first result, right after the search button. */
private const val FIRST_RESULT_INDEX = 1

private const val REFRESH_TIMEOUT_MILLIS = 30_000L
