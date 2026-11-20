package io.github.alxiw.wearpagingmediator.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.local.model.RepoEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoRemoteKeyEntity
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import io.github.alxiw.wearpagingmediator.data.local.model.SearchQueryResultEntity
import io.github.alxiw.wearpagingmediator.data.remote.api.GitHubApi
import io.github.alxiw.wearpagingmediator.data.remote.model.RepoSearchResponse
import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

/**
 * Guards the size of the Room cache: only the results of the [RecentQuery.MAX_CACHED_QUERIES]
 * most recently opened queries stay, the least recently opened one is deleted together with the
 * repositories and the topics no other query references.
 *
 * The number of queries used here is derived from the constant instead of being written down: every
 * test fills the cache exactly up to the limit and, when it expects an eviction, opens one query
 * more. Growing [RecentQuery.MAX_CACHED_QUERIES] does not invalidate these tests.
 */
@RunWith(AndroidJUnit4::class)
class RecentQueriesCacheTest {

    private lateinit var context: Context
    private lateinit var database: GitHubDatabase
    private lateinit var cache: RecentQueriesCache

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DB_NAME)
        database = Room.databaseBuilder(context, GitHubDatabase::class.java, TEST_DB_NAME).build()
        cache = RecentQueriesCache(database)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun queriesWithinTheLimitAreNotEvicted() = runBlocking {
        cacheQueriesWithinTheLimit()

        assertEquals(MAX_CACHED_QUERIES, database.searchQueriesDao().countQueries())
        assertEquals(QUERIES_WITHIN_THE_LIMIT, cachedQueries())
        QUERIES_WITHIN_THE_LIMIT.forEachIndexed { index, query ->
            assertTrue(isCached(query, REPO_IDS[index]))
        }
    }

    @Test
    fun queryBeyondTheLimitEvictsTheOneOpenedFirst() = runBlocking {
        cacheQueriesWithinTheLimit()

        cache(QUERY_BEYOND_THE_LIMIT, listOf(BEYOND_THE_LIMIT_REPO_ID))

        assertEquals(MAX_CACHED_QUERIES, database.searchQueriesDao().countQueries())
        assertEquals(
            QUERIES_WITHIN_THE_LIMIT.drop(1) + QUERY_BEYOND_THE_LIMIT,
            cachedQueries()
        )
        val evicted = QUERIES_WITHIN_THE_LIMIT.first()
        assertFalse(isCached(evicted, REPO_IDS.first()))
        // the rows of the evicted query are deleted, its repository is not referenced anymore
        assertNull(
            "the evicted query was the only one to reference this repository",
            database.reposDao().starsOf(REPO_IDS.first())
        )
        assertEquals(emptyList<String>(), database.reposDao().topicsOf(REPO_IDS.first()))
        QUERIES_WITHIN_THE_LIMIT.drop(1).forEachIndexed { index, query ->
            assertTrue(isCached(query, REPO_IDS[index + 1]))
        }
    }

    @Test
    fun queryOpenedAgainWithoutNetworkIsNotEvictedNext() = runBlocking {
        cacheQueriesWithinTheLimit()

        // offline the refresh cannot run, but opening the query must still mark it as used
        val reopened = QUERIES_WITHIN_THE_LIMIT.first()
        cache.touch(reopened)
        cache(QUERY_BEYOND_THE_LIMIT, listOf(BEYOND_THE_LIMIT_REPO_ID))

        assertTrue(isCached(reopened, REPO_IDS.first()))
        assertFalse(isCached(QUERIES_WITHIN_THE_LIMIT[1], REPO_IDS[1]))
        assertEquals(
            QUERIES_WITHIN_THE_LIMIT.drop(2) + reopened + QUERY_BEYOND_THE_LIMIT,
            cachedQueries()
        )
    }

    @Test
    fun repositorySharedWithAnotherQueryIsKept() = runBlocking {
        val evicted = QUERIES_WITHIN_THE_LIMIT.first()
        val kept = QUERIES_WITHIN_THE_LIMIT[1]
        cache(evicted, listOf(SHARED_REPO_ID, EVICTED_ONLY_REPO_ID))
        cache(kept, listOf(SHARED_REPO_ID))
        // fills the cache up to the limit, the query opened first is still the evicted one
        QUERIES_WITHIN_THE_LIMIT.drop(2).forEachIndexed { index, query ->
            cache(query, listOf(REPO_IDS[index + 2]))
        }
        assertEquals(MAX_CACHED_QUERIES, database.searchQueriesDao().countQueries())

        cache(QUERY_BEYOND_THE_LIMIT, listOf(BEYOND_THE_LIMIT_REPO_ID))

        assertEquals(0, database.reposDao().countSearchResults(evicted))
        assertNull(
            "only the evicted query referenced this repository",
            database.reposDao().starsOf(EVICTED_ONLY_REPO_ID)
        )
        assertEquals(emptyList<String>(), database.reposDao().topicsOf(EVICTED_ONLY_REPO_ID))
        assertEquals(100, database.reposDao().starsOf(SHARED_REPO_ID))
        assertEquals(listOf(0), database.reposDao().positionsFor(kept))
        assertEquals(listOf(topicOf(SHARED_REPO_ID)), database.reposDao().topicsOf(SHARED_REPO_ID))
    }

    @Test
    fun openingTheSameQueryAgainKeepsOneRow() = runBlocking {
        repeat(3) { cache.touch(REPEATED_QUERY) }

        assertEquals(1, database.searchQueriesDao().countQueries())
        assertEquals(listOf(REPEATED_QUERY), cachedQueries())
    }

    @Test
    fun openingAQueryRegistersItEvenIfTheRefreshFails() = runBlocking {
        val repository = ReposRepository(OfflineGitHubApi(), database, cache)

        withTimeout(TIMEOUT_MILLIS.milliseconds) { repository.getReposFlow(SearchQuery.of(REPEATED_QUERY)).first() }

        assertEquals(listOf(REPEATED_QUERY), cachedQueries())
    }

    @Test
    fun recentQueriesAreOfferedTheMostRecentlyUsedFirst() = runBlocking {
        cacheQueriesWithinTheLimit()
        val repository = ReposRepository(OfflineGitHubApi(), database, cache)

        val recent = repository.getRecentQueries()

        assertEquals(QUERIES_WITHIN_THE_LIMIT.reversed(), recent.map { it.query.value })
        recent.zipWithNext().forEach { (newer, older) ->
            assertTrue(
                "the query used later has to come first",
                newer.accessedAt.isAfter(older.accessedAt)
            )
        }
    }

    /** Caches [MAX_CACHED_QUERIES] queries, one repository each, and opens them in order. */
    private suspend fun cacheQueriesWithinTheLimit() {
        QUERIES_WITHIN_THE_LIMIT.forEachIndexed { index, query ->
            cache(query, listOf(REPO_IDS[index]))
        }
    }

    /** Caches the given results of [query] the way a refresh does, then opens the query. */
    private suspend fun cache(query: String, repoIds: List<Long>) {
        database.withTransaction {
            database.reposDao().upsertAll(repoIds.map(::repo))
            database.reposDao().insertSearchResults(
                repoIds.mapIndexed { position, repoId ->
                    SearchQueryResultEntity(query = query, repoId = repoId, position = position)
                }
            )
            database.remoteKeysDao().insertAll(
                repoIds.map { RepoRemoteKeyEntity(repoId = it, query = query, prevKey = null, nextKey = 2) }
            )
            database.reposDao().insertRepoTopics(
                repoIds.map { RepoTopicCrossRef(repoId = it, topic = topicOf(it)) }
            )
        }
        cache.touch(query)
    }

    private suspend fun isCached(query: String, repoId: Long): Boolean =
        database.reposDao().countSearchResults(query) > 0 &&
            database.reposDao().countRemoteKeys(query) > 0 &&
            database.reposDao().starsOf(repoId) != null

    private fun cachedQueries(): List<String> =
        database.openHelper.writableDatabase
            .query("SELECT `query` FROM `search_queries` ORDER BY `accessed_at`, `query`")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun repo(id: Long) = RepoEntity(
        id = id,
        name = "repo-$id",
        description = "Repository $id",
        url = "https://github.com/alxiw/repo-$id",
        stars = 100,
        forks = 10,
        language = "Kotlin"
    )

    private fun topicOf(repoId: Long) = "topic-$repoId"

    private class OfflineGitHubApi : GitHubApi {
        override suspend fun searchRepos(
            query: String,
            page: Int,
            pageSize: Int,
            sort: String,
            order: String
        ): RepoSearchResponse = throw IOException("no network")
    }

    private companion object {
        const val TEST_DB_NAME = "recent_queries_cache_test.db"
        const val TIMEOUT_MILLIS = 10_000L

        /** The value under test: everything below is derived from it. */
        const val MAX_CACHED_QUERIES = RecentQuery.MAX_CACHED_QUERIES

        /** Exactly fills the cache, the first one is the least recently opened query. */
        val QUERIES_WITHIN_THE_LIMIT = (1..MAX_CACHED_QUERIES).map { "query $it" }

        /** A repository per query, kept apart from the repositories the other tests share. */
        val REPO_IDS = QUERIES_WITHIN_THE_LIMIT.indices.map { (it + 1).toLong() }

        const val REPEATED_QUERY = "android"
        const val QUERY_BEYOND_THE_LIMIT = "one query too many"
        const val BEYOND_THE_LIMIT_REPO_ID = MAX_CACHED_QUERIES + 1L
        const val SHARED_REPO_ID = MAX_CACHED_QUERIES + 100L
        const val EVICTED_ONLY_REPO_ID = MAX_CACHED_QUERIES + 101L
    }
}
