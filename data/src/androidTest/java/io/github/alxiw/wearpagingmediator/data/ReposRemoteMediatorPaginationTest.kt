package io.github.alxiw.wearpagingmediator.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoResponse
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoSearchResponse
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards page alignment, the refresh of a cached window and the end of pagination.
 *
 * Every request and every stored position advances by [ReposRepository.NETWORK_PAGE_SIZE]. A
 * refresh replaces only the slice of its page: the cached head stays at position 0, so a cold start
 * still opens at the first result, and the pages after it stay cached until they are refreshed.
 *
 * GitHub search serves results only up to the offset of [ReposRemoteMediator.MAX_SEARCH_RESULTS]
 * and answers `HTTP 422` ("Only the first 1000 search results are available") for deeper pages,
 * even though `total_count` of a query like `Android` is millions. Appending therefore has to end
 * with a successful result instead of an error, and remote keys cached before this limit was
 * respected must not be followed.
 *
 * The window of pages kept in memory is bounded as well ([ReposRepository.pagingConfig] has a
 * finite `maxSize`), so pages can be dropped: when the remote key of the last item is gone the
 * mediator must not request anything.
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
class ReposRemoteMediatorPaginationTest {

    private lateinit var context: Context
    private lateinit var database: GitHubDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DB_NAME)
        database = Room.databaseBuilder(context, GitHubDatabase::class.java, TEST_DB_NAME).build()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun refreshRequestsExactlyOnePageFromTheFirstPosition() = runBlocking {
        val api = SearchApi(total = LARGE_TOTAL)
        val mediator = ReposRemoteMediator(QUERY, api, database)

        val result = mediator.load(LoadType.REFRESH, pagingState())
            as RemoteMediator.MediatorResult.Success

        assertFalse(result.endOfPaginationReached)
        assertEquals(listOf(1), api.requestedPages)
        assertEquals((0 until PAGE_SIZE).toList(), database.reposDao().positionsFor(QUERY.value))
        assertEquals(
            (0 until PAGE_SIZE).map { it.toLong() },
            cachedIds(loadSize = PAGE_SIZE)
        )
    }

    @Test
    fun refreshAtTheHeadKeepsTheFollowingPages() = runBlocking {
        cachePages(1..3)
        val api = SearchApi(total = LARGE_TOTAL)
        val mediator = ReposRemoteMediator(QUERY, api, database)

        val result = mediator.load(LoadType.REFRESH, refreshState(anchorPosition = 0))
            as RemoteMediator.MediatorResult.Success

        assertFalse(result.endOfPaginationReached)
        assertEquals(listOf(1), api.requestedPages)
        assertEquals((0 until 3 * PAGE_SIZE).toList(), database.reposDao().positionsFor(QUERY.value))
        assertEquals(3 * PAGE_SIZE, database.reposDao().countRemoteKeys(QUERY.value))
    }

    @Test
    fun refreshInTheMiddleKeepsTheCachedHeadAndTail() = runBlocking {
        cachePages(1..3)
        // The refreshed page has different repositories, so the untouched parts are visible.
        val api = SearchApi(total = LARGE_TOTAL, idShift = 1_000)
        val mediator = ReposRemoteMediator(QUERY, api, database)

        val result = mediator.load(LoadType.REFRESH, refreshState(anchorPosition = PAGE_SIZE))
            as RemoteMediator.MediatorResult.Success

        assertFalse(result.endOfPaginationReached)
        assertEquals(listOf(2), api.requestedPages)
        assertEquals((0 until 3 * PAGE_SIZE).toList(), database.reposDao().positionsFor(QUERY.value))
        val expectedIds = (0 until PAGE_SIZE).map { it.toLong() } +
            (1_030 until 1_060).map { it.toLong() } +
            (2 * PAGE_SIZE until 3 * PAGE_SIZE).map { it.toLong() }
        assertEquals(expectedIds, cachedIds())

        // A cold start must open the same first result, not the page that was refreshed.
        assertEquals(
            (0 until PAGE_SIZE).map { it.toLong() },
            cachedIds(loadSize = PAGE_SIZE)
        )
    }

    @Test
    fun refreshAtTheEndDropsStaleResultsWithoutAppending() = runBlocking {
        cachePages(1..4)
        // The search now has 45 results: the refreshed page 2 is the last, shorter page.
        val api = SearchApi(total = 45)
        val mediator = ReposRemoteMediator(QUERY, api, database)

        val result = mediator.load(LoadType.REFRESH, refreshState(anchorPosition = 45))
            as RemoteMediator.MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertEquals(listOf(2), api.requestedPages)
        assertEquals((0 until 45).toList(), database.reposDao().positionsFor(QUERY.value))
        assertEquals((0 until 45).map { it.toLong() }, cachedIds())
        assertEquals(45, database.reposDao().countRemoteKeys(QUERY.value))
    }

    @Test
    fun appendingStopsBeforeGitHubSearchResultLimit() = runBlocking {
        val api = SearchApi(total = LARGE_TOTAL)
        val mediator = ReposRemoteMediator(QUERY, api, database)

        var result = mediator.load(LoadType.REFRESH, pagingState()) as RemoteMediator.MediatorResult.Success
        var loads = 1
        while (!result.endOfPaginationReached) {
            assertTrue("pagination does not finish, $loads loads were made", loads < MAX_LOADS)
            result = mediator.load(LoadType.APPEND, appendState(api.lastReturnedId))
                as RemoteMediator.MediatorResult.Success
            loads++
        }

        assertEquals(
            "GitHub answers 422 for pages beyond the limit, they must not be requested",
            0,
            api.rejectedRequests
        )
        assertTrue(
            "the mediator has to paginate up to the limit, ${api.returnedIds.size} items were loaded",
            api.returnedIds.size >= ReposRemoteMediator.MAX_SEARCH_RESULTS
        )
        assertEquals(api.returnedIds.size, database.reposDao().countSearchResults(QUERY.value))
        // Positions stay contiguous, otherwise the Room PagingSource offsets would not match pages.
        assertEquals(
            api.returnedIds.indices.toList(),
            database.reposDao().positionsFor(QUERY.value)
        )
    }

    @Test
    fun cachedNextKeyBeyondSearchResultLimitIsNotRequested() = runBlocking {
        val api = SearchApi(total = LARGE_TOTAL)
        val mediator = ReposRemoteMediator(QUERY, api, database)
        database.reposDao().upsertAll(listOf(repoEntity(CACHED_REPO_ID)))
        database.reposDao().insertSearchResults(
            listOf(
                SearchQueryResultEntity(
                    query = QUERY.value,
                    repoId = CACHED_REPO_ID,
                    position = 0
                )
            )
        )
        // The last page GitHub serves is 34: a key cached before the limit was respected can point
        // to page 35, which always ends with HTTP 422.
        database.remoteKeysDao().insertAll(
            listOf(
                RepoRemoteKeyEntity(
                    repoId = CACHED_REPO_ID,
                    query = QUERY.value,
                    prevKey = LAST_SERVED_PAGE - 1,
                    nextKey = LAST_SERVED_PAGE + 1
                )
            )
        )

        val result = mediator.load(LoadType.APPEND, appendState(CACHED_REPO_ID))
            as RemoteMediator.MediatorResult.Success

        assertTrue(result.endOfPaginationReached)
        assertEquals(0, api.requests)
    }

    @Test
    fun appendWithoutRemoteKeyDoesNotRequestNetwork() = runBlocking {
        val api = SearchApi(total = LARGE_TOTAL)
        val mediator = ReposRemoteMediator(QUERY, api, database)

        // The page was dropped from the bounded window, so its remote key is unknown here.
        val result = mediator.load(LoadType.APPEND, appendState(UNKNOWN_REPO_ID))
            as RemoteMediator.MediatorResult.Success

        assertFalse(result.endOfPaginationReached)
        assertEquals(0, api.requests)
    }

    @Test
    fun pagingWindowIsBounded() {
        val config = ReposRepository.pagingConfig

        assertNotEquals("pages have to be dropped from memory", Int.MAX_VALUE, config.maxSize)
        assertTrue(
            "the initial load has to fit into the window, otherwise it is dropped immediately",
            config.maxSize >= config.initialLoadSize
        )
        assertFalse(config.enablePlaceholders)
    }

    /**
     * The remote key page, the GitHub request and the Room positions all advance by one page size:
     * a refresh must not load two pages while its key points to one. The prefetch distance stays
     * below a page, so restoring a scroll position does not append in the background.
     */
    @Test
    fun theFirstLoadIsOnePage() {
        val config = ReposRepository.pagingConfig

        assertEquals(
            "the request, the remote key and the stored positions must use the same page size",
            ReposRepository.NETWORK_PAGE_SIZE,
            config.initialLoadSize
        )
        assertTrue(
            "the window has to hold the initial load twice, otherwise its pages are dropped at once",
            config.maxSize >= config.initialLoadSize * 2
        )
        assertTrue(
            "restoring a scroll position must not append, so the prefetch stays below one page",
            config.prefetchDistance < config.pageSize
        )
    }

    /** Caches full pages that look like they were loaded from the network before this test. */
    private suspend fun cachePages(pages: IntRange) {
        database.withTransaction {
            pages.forEach { page ->
                val startPosition = (page - 1) * PAGE_SIZE
                val repos = idsOfPage(page).map(::repoEntity)
                database.reposDao().upsertAll(repos)
                database.remoteKeysDao().insertAll(
                    repos.map { repo ->
                        RepoRemoteKeyEntity(
                            repoId = repo.id,
                            query = QUERY.value,
                            prevKey = if (page == 1) null else page - 1,
                            nextKey = page + 1
                        )
                    }
                )
                database.reposDao().insertSearchResults(
                    repos.mapIndexed { index, repo ->
                        SearchQueryResultEntity(
                            query = QUERY.value,
                            repoId = repo.id,
                            position = startPosition + index
                        )
                    }
                )
            }
        }
    }

    private fun refreshState(anchorPosition: Int, cachedPages: IntRange = 1..3) =
        PagingState<Int, RepoEntity>(
            pages = cachedPages.map { page ->
                PagingSource.LoadResult.Page<Int, RepoEntity>(
                    data = idsOfPage(page).map(::repoEntity),
                    prevKey = null,
                    nextKey = null
                )
            },
            anchorPosition = anchorPosition,
            config = ReposRepository.pagingConfig,
            leadingPlaceholderCount = 0
        )

    /** Reads the cached results in the order of the PagingSource, i.e. the cold start order. */
    private suspend fun cachedIds(loadSize: Int = ReposRemoteMediator.MAX_SEARCH_RESULTS): List<Long> {
        val source = database.reposDao().getReposForQuery(QUERY.value)
        return try {
            val result = source.load(
                PagingSource.LoadParams.Refresh<Int>(
                    key = null,
                    loadSize = loadSize,
                    placeholdersEnabled = false
                )
            )
            (result as PagingSource.LoadResult.Page<Int, RepoEntity>).data.map { it.id }
        } finally {
            source.invalidate()
        }
    }

    private fun idsOfPage(page: Int): List<Long> {
        val startPosition = (page - 1) * PAGE_SIZE
        return (startPosition until startPosition + PAGE_SIZE).map { it.toLong() }
    }

    private fun pagingState() = PagingState<Int, RepoEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = ReposRepository.pagingConfig,
        leadingPlaceholderCount = 0
    )

    private fun appendState(lastRepoId: Long) = PagingState<Int, RepoEntity>(
        pages = listOf(
            PagingSource.LoadResult.Page(
                data = listOf(repoEntity(lastRepoId)),
                prevKey = null,
                nextKey = null
            )
        ),
        anchorPosition = 0,
        config = ReposRepository.pagingConfig,
        leadingPlaceholderCount = 0
    )

    private fun repoEntity(id: Long) = RepoEntity(
        id = id,
        name = "repo-$id",
        description = "Repository $id",
        url = "https://github.com/alxiw/repo-$id",
        stars = 100,
        forks = 10,
        language = "Kotlin"
    )

    /**
     * Mimics GitHub search: returns [RepoResponse]s by offset and fails with the same HTTP 422 the
     * real endpoint answers for pages at or beyond [maxResults] offset.
     */
    private class SearchApi(
        private val total: Int,
        private val maxResults: Int = ReposRemoteMediator.MAX_SEARCH_RESULTS,
        private val idShift: Long = 0
    ) : GitHubApi {

        val returnedIds = mutableListOf<Long>()
        val requestedPages = mutableListOf<Int>()
        var requests = 0
            private set
        var rejectedRequests = 0
            private set

        val lastReturnedId: Long
            get() = returnedIds.last()

        override suspend fun searchRepos(
            query: String,
            page: Int,
            pageSize: Int,
            sort: String,
            order: String
        ): RepoSearchResponse {
            val offset = (page - 1) * pageSize
            requests++
            requestedPages += page
            if (offset >= maxResults) {
                rejectedRequests++
                throw IllegalStateException("HTTP 422 Only the first $maxResults search results are available")
            }
            val end = minOf(offset + pageSize, maxResults, total)
            val ids = if (offset >= total) {
                emptyList()
            } else {
                (offset until end).map { (it + idShift).toLong() }
            }
            returnedIds += ids
            return RepoSearchResponse(total = total, items = ids.map { repoResponse(it) })
        }

        private fun repoResponse(id: Long) = RepoResponse(
            id = id,
            name = "repo-$id",
            fullName = "alxiw/repo-$id",
            description = "Repository $id",
            url = "https://github.com/alxiw/repo-$id",
            stars = 100,
            forks = 10,
            language = "Kotlin",
            topics = listOf("android", "paging")
        )
    }

    private companion object {
        const val TEST_DB_NAME = "repos_remote_mediator_pagination_test.db"
        val QUERY = SearchQuery.of("android")
        const val LARGE_TOTAL = 2_000_000
        const val PAGE_SIZE = ReposRepository.NETWORK_PAGE_SIZE
        const val MAX_LOADS = 100
        const val CACHED_REPO_ID = 990L
        const val UNKNOWN_REPO_ID = 987_654L

        // Pages are served while `(page - 1) * pageSize < 1000`, so the last one is 34.
        const val LAST_SERVED_PAGE = 34
    }
}
