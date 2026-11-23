package io.github.alxiw.wearpagingmediator.data

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the refresh key of the Room source. Paging reports its anchor relative to the window that
 * is still loaded, while Room keys are absolute positions in the query, so the key has to be moved
 * by the number of items the window no longer holds.
 */
@RunWith(AndroidJUnit4::class)
class ReposPagingSourceTest {

    @Test
    fun refreshKeyIsAbsoluteWhenTheHeadWasDropped() {
        val source = ReposPagingSource(DelegateSource(roomKey = 9))

        assertEquals(18, source.getRefreshKey(pagingState(anchorPosition = 24, itemsBefore = 9)))
    }

    @Test
    fun refreshKeyStaysTheSameWhenTheWholeListIsLoaded() {
        val source = ReposPagingSource(DelegateSource(roomKey = 18))

        assertEquals(18, source.getRefreshKey(pagingState(anchorPosition = 33, itemsBefore = 0)))
    }

    @Test
    fun noAnchorHasNoRefreshKey() {
        val source = ReposPagingSource(DelegateSource(roomKey = null))

        assertEquals(null, source.getRefreshKey(pagingState(anchorPosition = 24, itemsBefore = 9)))
    }

    private fun pagingState(anchorPosition: Int, itemsBefore: Int): PagingState<Int, RepoEntity> =
        PagingState(
            pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(repoEntity()),
                    prevKey = null,
                    nextKey = 30,
                    itemsBefore = itemsBefore,
                    itemsAfter = 51
                )
            ),
            anchorPosition = anchorPosition,
            config = PagingConfig(pageSize = 30, initialLoadSize = 30),
            leadingPlaceholderCount = 0
        )

    private fun repoEntity() = RepoEntity(
        id = 1L,
        name = "repo",
        description = null,
        url = "https://github.com/alxiw/repo",
        stars = 1,
        forks = 1,
        language = null
    )

    private class DelegateSource(private val roomKey: Int?) : PagingSource<Int, RepoEntity>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RepoEntity> =
            LoadResult.Page(data = emptyList(), prevKey = null, nextKey = null)

        override fun getRefreshKey(state: PagingState<Int, RepoEntity>): Int? = roomKey
    }
}
