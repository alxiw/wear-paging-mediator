package io.github.alxiw.wearpagingmediator.data

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoResponse
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoSearchResponse
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for the shared `repos` table: a refresh of one search query re-inserts
 * repositories that can already be cached for another query, and it must not wipe that other
 * query's rows from `search_query_results`.
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(AndroidJUnit4::class)
class ReposRemoteMediatorCacheTest {

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
    fun refreshOfAnotherQueryKeepsCachedResultsOfPreviousQuery() = runBlocking {
        val api = FakeGitHubApi(listOf(repoResponse(FIRST_REPO_ID), repoResponse(SECOND_REPO_ID)))
        val androidMediator = ReposRemoteMediator(QUERY_ANDROID, api, database)
        val kotlinMediator = ReposRemoteMediator(QUERY_KOTLIN, api, database)

        assertTrue(androidMediator.load(LoadType.REFRESH, pagingState()) is RemoteMediator.MediatorResult.Success)
        assertEquals(2, database.reposDao().countSearchResults(QUERY_ANDROID))

        // Both queries return the same repositories, so this refresh re-inserts existing rows.
        assertTrue(kotlinMediator.load(LoadType.REFRESH, pagingState()) is RemoteMediator.MediatorResult.Success)

        assertEquals(2, database.reposDao().countSearchResults(QUERY_ANDROID))
        assertEquals(2, database.reposDao().countSearchResults(QUERY_KOTLIN))
        assertEquals(2, database.reposDao().countRemoteKeys(QUERY_ANDROID))
        assertEquals(2, database.reposDao().countRemoteKeys(QUERY_KOTLIN))
    }

    @Test
    fun refreshReplacesResultsOfTheSameQueryWithoutDuplicates() = runBlocking {
        val mediator = ReposRemoteMediator(
            QUERY_ANDROID,
            FakeGitHubApi(listOf(repoResponse(FIRST_REPO_ID, stars = 100))),
            database
        )
        assertTrue(mediator.load(LoadType.REFRESH, pagingState()) is RemoteMediator.MediatorResult.Success)

        val refreshed = ReposRemoteMediator(
            QUERY_ANDROID,
            FakeGitHubApi(listOf(repoResponse(FIRST_REPO_ID, stars = 500), repoResponse(SECOND_REPO_ID, stars = 200))),
            database
        )
        assertTrue(refreshed.load(LoadType.REFRESH, pagingState()) is RemoteMediator.MediatorResult.Success)

        assertEquals(2, database.reposDao().countSearchResults(QUERY_ANDROID))
        assertEquals(listOf(0, 1), database.reposDao().positionsFor(QUERY_ANDROID))
        assertEquals(500, database.reposDao().starsOf(FIRST_REPO_ID))
    }

    private fun pagingState() = PagingState<Int, RepoEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE * 3),
        leadingPlaceholderCount = 0
    )

    private fun repoResponse(id: Long, stars: Int = 100) = RepoResponse(
        id = id,
        name = "repo-$id",
        fullName = "alxiw/repo-$id",
        description = "Repository $id",
        url = "https://github.com/alxiw/repo-$id",
        stars = stars,
        forks = 10,
        language = "Kotlin",
        topics = listOf("android", "paging")
    )

    private class FakeGitHubApi(
        private val items: List<RepoResponse>
    ) : GitHubApi {
        override suspend fun searchRepos(
            query: String,
            page: Int,
            pageSize: Int,
            sort: String,
            order: String
        ): RepoSearchResponse = RepoSearchResponse(total = items.size, items = items)
    }

    private companion object {
        const val TEST_DB_NAME = "repos_remote_mediator_cache_test.db"
        const val QUERY_ANDROID = "android"
        const val QUERY_KOTLIN = "kotlin"
        const val FIRST_REPO_ID = 1L
        const val SECOND_REPO_ID = 2L
        const val PAGE_SIZE = 30
    }
}
