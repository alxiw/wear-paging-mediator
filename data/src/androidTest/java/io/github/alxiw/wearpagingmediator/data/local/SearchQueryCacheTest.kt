package io.github.alxiw.wearpagingmediator.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.data.local.dao.ReposDao
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the per-query search cache: the `repos` table is shared between all search queries,
 * so re-inserting a repository while refreshing one query must not remove the same repository
 * from the cached results of other queries (`search_query_results` has a cascading foreign key).
 *
 * The database is file based on purpose: [Room] enables `PRAGMA foreign_keys` for real
 * databases, which is exactly what makes the cascade delete visible.
 */
@RunWith(AndroidJUnit4::class)
class SearchQueryCacheTest {

    private lateinit var context: Context
    private lateinit var database: GitHubDatabase
    private lateinit var dao: ReposDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DB_NAME)
        database = Room.databaseBuilder(context, GitHubDatabase::class.java, TEST_DB_NAME).build()
        dao = database.reposDao()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun foreignKeysAreEnabled() = runBlocking {
        val enabled = database.openHelper.writableDatabase
            .query("PRAGMA foreign_keys")
            .use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }

        assertTrue("Room has to enable foreign keys, otherwise the cascade delete is not applied", enabled)
    }

    @Test
    fun reinsertingRepoKeepsSearchResultsOfOtherQueries() = runBlocking {
        dao.upsertAll(listOf(repo(stars = 100)))
        dao.insertSearchResults(
            listOf(
                SearchQueryResultEntity(query = QUERY_ANDROID, repoId = REPO_ID, position = 0),
                SearchQueryResultEntity(query = QUERY_KOTLIN, repoId = REPO_ID, position = 0)
            )
        )

        // A refresh of another query upserts the same repository with fresh data.
        dao.upsertAll(listOf(repo(stars = 200)))

        assertEquals(1, dao.countSearchResults(QUERY_ANDROID))
        assertEquals(1, dao.countSearchResults(QUERY_KOTLIN))
        assertEquals(200, dao.starsOf(REPO_ID))
    }

    @Test
    fun deletingResultsForOneQueryKeepsOtherQueries() = runBlocking {
        dao.upsertAll(listOf(repo()))
        dao.insertSearchResults(
            listOf(
                SearchQueryResultEntity(query = QUERY_ANDROID, repoId = REPO_ID, position = 0),
                SearchQueryResultEntity(query = QUERY_KOTLIN, repoId = REPO_ID, position = 0)
            )
        )

        dao.deleteResultsForQuery(QUERY_KOTLIN)

        assertEquals(0, dao.countSearchResults(QUERY_KOTLIN))
        assertEquals(1, dao.countSearchResults(QUERY_ANDROID))
    }

    @Test
    fun remoteKeysOfOtherQueriesSurviveRepoReinsert() = runBlocking {
        dao.upsertAll(listOf(repo()))
        dao.insertSearchResults(
            listOf(SearchQueryResultEntity(query = QUERY_ANDROID, repoId = REPO_ID, position = 0))
        )
        database.remoteKeysDao().insertAll(
            listOf(
                RepoRemoteKeyEntity(
                    repoId = REPO_ID,
                    query = QUERY_ANDROID,
                    prevKey = null,
                    nextKey = 2
                )
            )
        )

        dao.upsertAll(listOf(repo(stars = 300)))

        assertEquals(1, dao.countSearchResults(QUERY_ANDROID))
        assertEquals(1, dao.countRemoteKeys(QUERY_ANDROID))
    }

    private fun repo(stars: Int = 100) = RepoEntity(
        id = REPO_ID,
        name = "alxiw/wear-paging-mediator",
        description = "Paging 3 + RemoteMediator sample",
        url = "https://github.com/alxiw/wear-paging-mediator",
        stars = stars,
        forks = 10,
        language = "Kotlin"
    )

    private companion object {
        const val TEST_DB_NAME = "search_query_cache_test.db"
        const val REPO_ID = 42L
        const val QUERY_ANDROID = "android"
        const val QUERY_KOTLIN = "kotlin"
    }
}
