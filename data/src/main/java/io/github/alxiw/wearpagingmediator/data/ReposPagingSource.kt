package io.github.alxiw.wearpagingmediator.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity

/**
 * Keeps the position of a refreshed Room source absolute.
 *
 * Room starts a refresh half of the initial load before [PagingState.anchorPosition], but that
 * anchor is an index inside the loaded window, not the offset stored in Room. Once pages before
 * the window were dropped, the key Room returns is short by the number of those items and the
 * refreshed window begins far above the result the list was anchored on. The [itemsBefore] of the
 * first loaded page restores the absolute offset, so a refresh keeps the window around the anchor
 * no matter how much of the head Paging has already dropped.
 */
internal class ReposPagingSource(
    private val source: PagingSource<Int, RepoEntity>
) : PagingSource<Int, RepoEntity>() {

    init {
        registerInvalidatedCallback { source.invalidate() }
        source.registerInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RepoEntity> =
        source.load(params)

    override fun getRefreshKey(state: PagingState<Int, RepoEntity>): Int? {
        val roomKey = source.getRefreshKey(state) ?: return null
        val droppedHead = state.pages.firstOrNull()?.itemsBefore?.coerceAtLeast(0) ?: 0
        return roomKey + droppedHead
    }

    override val jumpingSupported: Boolean
        get() = source.jumpingSupported

    override val keyReuseSupported: Boolean
        get() = source.keyReuseSupported
}
