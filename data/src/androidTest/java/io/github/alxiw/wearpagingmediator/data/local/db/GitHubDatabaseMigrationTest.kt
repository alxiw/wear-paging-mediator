package io.github.alxiw.wearpagingmediator.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real migration on the real schema of every version (see `schemas`, exported by Room):
 * [MigrationTestHelper.runMigrationsAndValidate] validates the migrated database against `5.json`,
 * so a mismatch between the migration and the entities fails here, not on a user device.
 */
@RunWith(AndroidJUnit4::class)
class GitHubDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GitHubDatabase::class.java
    )

    @Before
    @After
    fun deleteDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun migrationRenamesColumnsToSnakeCase() {
        helper.createDatabase(TEST_DB_NAME, 4).close()

        val database = migrateTo5()

        assertEquals(listOf("query", "repo_id", "position"), database.columns("search_query_results"))
        assertEquals(listOf("repo_id", "query", "prev_key", "next_key"), database.columns("repo_remote_keys"))
        assertEquals(listOf("repo_id", "topic"), database.columns("repo_topic_cross_ref"))
    }

    @Test
    fun migrationKeepsCachedData() {
        helper.createDatabase(TEST_DB_NAME, 4).apply {
            insertRepo(REPO_ID)
            insertRepo(OTHER_REPO_ID)
            insertResult(QUERY_ANDROID, REPO_ID, position = 0)
            insertResult(QUERY_ANDROID, OTHER_REPO_ID, position = 1)
            insertKey(REPO_ID, QUERY_ANDROID, nextKey = 2)
            insertKey(OTHER_REPO_ID, QUERY_ANDROID, nextKey = 2)
            insertTopic(REPO_ID, "android")
            insertTopic(REPO_ID, "paging")
            close()
        }

        val database = migrateTo5()

        assertEquals(listOf(0, 1), database.positions(QUERY_ANDROID))
        assertEquals(2, database.count("repo_remote_keys"))
        assertEquals(2, database.count("repo_topic_cross_ref"))
        assertEquals(listOf("android", "paging"), database.topics(REPO_ID))
        assertEquals(100, database.stars(REPO_ID))
        assertEquals(listOf(QUERY_ANDROID), database.queries())
    }

    @Test
    fun migrationDropsTopicsTable() {
        helper.createDatabase(TEST_DB_NAME, 4).apply {
            execSQL("INSERT INTO `topics` (`topic`) VALUES (?)", arrayOf<Any?>("android"))
            close()
        }

        val database = migrateTo5()

        assertFalse(
            "topics is write-only, the repositories read their topics from repo_topic_cross_ref",
            database.tableNames().contains("topics")
        )
    }

    @Test
    fun migrationRenamesResultsIndex() {
        helper.createDatabase(TEST_DB_NAME, 4).close()

        val database = migrateTo5()

        val indices = database.indexNames("search_query_results")
        assertTrue(indices.contains("index_search_query_results_repo_id"))
        assertFalse(indices.contains("index_search_query_results_repoId"))
    }

    @Test
    fun migrationSeedsQueriesFromCachedData() {
        helper.createDatabase(TEST_DB_NAME, 4).apply {
            insertRepo(REPO_ID)
            // cached results only
            insertResult(QUERY_ANDROID, REPO_ID, position = 0)
            // both results and page keys, the query must not be seeded twice
            insertResult(QUERY_KOTLIN, REPO_ID, position = 0)
            insertKey(REPO_ID, QUERY_KOTLIN, nextKey = 2)
            // page keys only
            insertKey(REPO_ID, QUERY_RUST, nextKey = 2)
            close()
        }

        val database = migrateTo5()

        assertEquals(listOf(QUERY_ANDROID, QUERY_KOTLIN, QUERY_RUST), database.queries())
        assertEquals(0L, database.accessedAt(QUERY_KOTLIN))
    }

    @Test
    fun migrationKeepsFiveQueries() {
        // 5 is the limit of version 5 itself, not the constant of the cache: growing the cache in a
        // later version must not change what this migration does to an existing database.
        helper.createDatabase(TEST_DB_NAME, 4).apply {
            (1..7).forEach { index ->
                insertRepo(index.toLong())
                insertResult("query $index", index.toLong(), position = 0)
                insertKey(index.toLong(), "query $index", nextKey = 2)
            }
            close()
        }

        val database = migrateTo5()

        // the moment a cached query was used is not stored, all of them are equally old: the two
        // that are dropped are picked by name
        assertEquals(
            listOf("query 3", "query 4", "query 5", "query 6", "query 7"),
            database.queries()
        )
        assertEquals(5, database.count("search_query_results"))
        assertEquals(5, database.count("repo_remote_keys"))
        assertEquals(5, database.count("repos"))
    }

    @Test
    fun migrationDeletesOnlyRowsTheEvictedQueryReferenced() {
        // 6 queries: with the limit of 5 the least recently used one, "query 1", is evicted
        helper.createDatabase(TEST_DB_NAME, 4).apply {
            (1..6).forEach { index ->
                val repoId = FILLER_REPO_IDS[index - 1]
                insertRepo(repoId)
                insertResult("query $index", repoId, position = 0)
                insertKey(repoId, "query $index", nextKey = 2)
            }
            // only in the evicted query
            insertRepo(ONLY_EVICTED_REPO_ID)
            insertResult(EVICTED_QUERY, ONLY_EVICTED_REPO_ID, position = 1)
            insertTopic(ONLY_EVICTED_REPO_ID, "android")
            // in the evicted query and in a kept one
            insertRepo(SHARED_REPO_ID)
            insertResult(EVICTED_QUERY, SHARED_REPO_ID, position = 2)
            insertResult(QUERY_TWO, SHARED_REPO_ID, position = 1)
            insertTopic(SHARED_REPO_ID, "runtime")
            // only page keys of a kept query, no cached result
            insertRepo(KEYS_ONLY_REPO_ID)
            insertKey(KEYS_ONLY_REPO_ID, QUERY_TWO, nextKey = 2)
            insertTopic(KEYS_ONLY_REPO_ID, "compose")
            // referenced by nothing at all
            insertRepo(UNREFERENCED_REPO_ID)
            insertTopic(UNREFERENCED_REPO_ID, "wear")
            close()
        }

        val database = migrateTo5()

        val expectedRepos = FILLER_REPO_IDS.drop(1) + SHARED_REPO_ID + KEYS_ONLY_REPO_ID
        assertEquals(expectedRepos.sorted(), database.repoIds())
        assertEquals(listOf("runtime"), database.topics(SHARED_REPO_ID))
        assertEquals(listOf("compose"), database.topics(KEYS_ONLY_REPO_ID))
    }

    private fun migrateTo5(): SupportSQLiteDatabase =
        helper.runMigrationsAndValidate(TEST_DB_NAME, 5, true, GitHubDatabase.MIGRATION_4_5)

    private fun SupportSQLiteDatabase.insertRepo(id: Long, stars: Int = 100) = execSQL(
        "INSERT INTO `repos` (`id`, `name`, `description`, `url`, `stars`, `forks`, `language`) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
        arrayOf<Any?>(id, "repo-$id", "Repository $id", "https://github.com/alxiw/repo-$id", stars, 10, "Kotlin")
    )

    private fun SupportSQLiteDatabase.insertResult(query: String, repoId: Long, position: Int) = execSQL(
        "INSERT INTO `search_query_results` (`query`, `repoId`, `position`) VALUES (?, ?, ?)",
        arrayOf<Any?>(query, repoId, position)
    )

    private fun SupportSQLiteDatabase.insertKey(
        repoId: Long,
        query: String,
        prevKey: Int? = null,
        nextKey: Int? = null
    ) = execSQL(
        "INSERT INTO `repo_remote_keys` (`repoId`, `query`, `prevKey`, `nextKey`) VALUES (?, ?, ?, ?)",
        arrayOf<Any?>(repoId, query, prevKey, nextKey)
    )

    private fun SupportSQLiteDatabase.insertTopic(repoId: Long, topic: String) = execSQL(
        "INSERT INTO `repo_topic_cross_ref` (`id`, `topic`) VALUES (?, ?)",
        arrayOf<Any?>(repoId, topic)
    )

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM `$table`").use { cursor ->
            check(cursor.moveToFirst()) { "the count query returned no row" }
            cursor.getInt(0)
        }

    private fun SupportSQLiteDatabase.positions(queryText: String): List<Int> =
        query(
            "SELECT `position` FROM `search_query_results` WHERE `query` = ? ORDER BY `position`",
            arrayOf<Any?>(queryText)
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getInt(0)) } }

    private fun SupportSQLiteDatabase.topics(repoId: Long): List<String> =
        query(
            "SELECT `topic` FROM `repo_topic_cross_ref` WHERE `repo_id` = ? ORDER BY `topic`",
            arrayOf<Any?>(repoId)
        ).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun SupportSQLiteDatabase.stars(repoId: Long): Int? =
        query("SELECT `stars` FROM `repos` WHERE `id` = ?", arrayOf<Any?>(repoId)).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else null
        }

    private fun SupportSQLiteDatabase.repoIds(): List<Long> =
        query("SELECT `id` FROM `repos` ORDER BY `id`")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }

    private fun SupportSQLiteDatabase.queries(): List<String> =
        query("SELECT `query` FROM `search_queries` ORDER BY `accessed_at`, `query`")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun SupportSQLiteDatabase.accessedAt(queryText: String): Long? =
        query("SELECT `accessed_at` FROM `search_queries` WHERE `query` = ?", arrayOf<Any?>(queryText))
            .use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else null }

    private fun SupportSQLiteDatabase.columns(table: String): List<String> =
        query("PRAGMA table_info(`$table`)").use { cursor ->
            val name = cursor.getColumnIndexOrThrow("name")
            buildList { while (cursor.moveToNext()) add(cursor.getString(name)) }
        }

    private fun SupportSQLiteDatabase.tableNames(): List<String> =
        query("SELECT `name` FROM `sqlite_master` WHERE `type` = 'table'")
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun SupportSQLiteDatabase.indexNames(table: String): List<String> =
        query("SELECT `name` FROM `sqlite_master` WHERE `type` = 'index' AND `tbl_name` = ?", arrayOf<Any?>(table))
            .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private companion object {
        const val TEST_DB_NAME = "migration_test.db"

        const val REPO_ID = 1L
        const val OTHER_REPO_ID = 2L
        const val ONLY_EVICTED_REPO_ID = 900L
        const val SHARED_REPO_ID = 901L
        const val KEYS_ONLY_REPO_ID = 902L
        const val UNREFERENCED_REPO_ID = 903L
        val FILLER_REPO_IDS = (100L..105L).toList()

        const val QUERY_ANDROID = "android"
        const val QUERY_KOTLIN = "kotlin"
        const val QUERY_RUST = "rust"
        const val EVICTED_QUERY = "query 1"
        const val QUERY_TWO = "query 2"
    }
}
