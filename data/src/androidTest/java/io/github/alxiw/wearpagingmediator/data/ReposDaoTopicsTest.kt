package io.github.alxiw.wearpagingmediator.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.alxiw.wearpagingmediator.data.local.db.GitHubDatabase
import io.github.alxiw.wearpagingmediator.data.local.model.RepoTopicCrossRef
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Guards how the cached topics of a repo are read and replaced. */
@RunWith(AndroidJUnit4::class)
class ReposDaoTopicsTest {

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
    fun topicsOfARepoAreSortedWithoutCase() = runBlocking {
        val dao = database.reposDao()
        dao.insertRepoTopics(
            listOf(
                RepoTopicCrossRef(REPO_ID, "wear-os"),
                RepoTopicCrossRef(REPO_ID, "Android"),
                RepoTopicCrossRef(REPO_ID, "paging")
            )
        )

        assertEquals(listOf("Android", "paging", "wear-os"), dao.topicsOf(REPO_ID))
    }

    @Test
    fun topicsOfAnotherRepoAreNotRead() = runBlocking {
        val dao = database.reposDao()
        dao.insertRepoTopics(
            listOf(
                RepoTopicCrossRef(REPO_ID, "android"),
                RepoTopicCrossRef(OTHER_REPO_ID, "kotlin")
            )
        )

        assertEquals(listOf("android"), dao.topicsOf(REPO_ID))
    }

    @Test
    fun deletedTopicsAreGoneAndTheOthersAreKept() = runBlocking {
        val dao = database.reposDao()
        dao.insertRepoTopics(
            listOf(
                RepoTopicCrossRef(REPO_ID, "android"),
                RepoTopicCrossRef(OTHER_REPO_ID, "kotlin")
            )
        )

        dao.deleteRepoTopics(listOf(REPO_ID))

        assertEquals(emptyList<String>(), dao.topicsOf(REPO_ID))
        assertEquals(listOf("kotlin"), dao.topicsOf(OTHER_REPO_ID))
    }

    private companion object {
        const val TEST_DB_NAME = "repos_dao_topics_test.db"
        const val REPO_ID = 1L
        const val OTHER_REPO_ID = 2L
    }
}
