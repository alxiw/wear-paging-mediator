package io.github.alxiw.wearpagingmediator.presentation.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingSource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material3.MaterialTheme
import io.github.alxiw.wearpagingmediator.domain.GetReposFlowUseCase
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.presentation.Constants.DEFAULT_QUERY
import io.github.alxiw.wearpagingmediator.presentation.MainViewModel
import io.github.alxiw.wearpagingmediator.presentation.R
import io.github.alxiw.wearpagingmediator.presentation.model.MainIntent
import io.github.alxiw.wearpagingmediator.presentation.testing.BAND_CAPS
import io.github.alxiw.wearpagingmediator.presentation.testing.FakeRepository
import io.github.alxiw.wearpagingmediator.presentation.testing.FakeSearchResults
import io.github.alxiw.wearpagingmediator.presentation.testing.FIRST_REPO
import io.github.alxiw.wearpagingmediator.presentation.testing.REPOS
import io.github.alxiw.wearpagingmediator.presentation.testing.RecordingContext
import io.github.alxiw.wearpagingmediator.presentation.testing.THIRD_REPO
import io.github.alxiw.wearpagingmediator.presentation.testing.awaitCondition
import io.github.alxiw.wearpagingmediator.presentation.testing.awaitNode
import io.github.alxiw.wearpagingmediator.presentation.testing.label
import io.github.alxiw.wearpagingmediator.presentation.testing.page
import io.github.alxiw.wearpagingmediator.presentation.testing.repo
import io.github.alxiw.wearpagingmediator.presentation.util.CountFormatter
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the screen as a whole: what it reports around the results, how a query is made and what
 * happens to the results of the queries that were visited before.
 *
 * The ViewModel is built by hand, so the screen is tested without the graph of the app, and its
 * search is answered by [FakeSearchResults] instead of the voice input of the watch.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var repository: FakeRepository
    private lateinit var viewModel: MainViewModel
    private lateinit var context: RecordingContext

    // region states of the list

    @Test
    fun defaultQueryIsShownOnTheSearchButton() {
        showScreen()

        rule.onNodeWithText(DEFAULT_QUERY.value).assertIsDisplayed()
        rule.awaitCondition { repository.queries == listOf(DEFAULT_QUERY) }
    }

    @Test
    fun resultsAreGroupedByStarBands() {
        showScreen()

        scrollTo(FIRST_REPO.name)
        BAND_CAPS.forEach { scrollTo(bandLabel(it)) }
        scrollTo(THIRD_REPO.name)
    }

    @Test
    fun firstLoadIsNotReportedAsAnEmptyList() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeRepository().apply {
            load = { _, _ ->
                gate.await()
                page(REPOS)
            }
        }

        showScreen(repository = repository)
        rule.awaitCondition { this.repository.loads.isNotEmpty() }

        // the query has nothing to show yet, but the head of the list is not taken by a message
        rule.onNodeWithText(label(R.string.empty)).assertDoesNotExist()
        rule.onNodeWithText(label(R.string.end_of_list)).assertDoesNotExist()

        gate.complete(Unit)

        scrollTo(FIRST_REPO.name)
    }

    @Test
    fun searchWithoutResultsIsReported() {
        showScreen(
            repository = FakeRepository().apply {
                load = { _, _ -> page(emptyList()) }
            }
        )

        scrollTo(label(R.string.empty))
    }

    @Test
    fun endOfTheListIsReportedAfterTheLastResult() {
        showScreen()

        scrollTo(THIRD_REPO.name)
        scrollTo(label(R.string.end_of_list))
    }

    @Test
    fun loadingMoreDoesNotReportTheEndOfTheList() {
        val gate = CompletableDeferred<Unit>()
        val fourthRepo = repo(id = 4, name = "fourth-repo")
        val repository = FakeRepository().apply {
            load = { _, params ->
                if (params is PagingSource.LoadParams.Append) {
                    gate.await()
                    page(listOf(fourthRepo))
                } else {
                    page(REPOS, nextKey = 1)
                }
            }
        }

        showScreen(repository = repository)
        scrollTo(FIRST_REPO.name)
        awaitAppend()

        // the next page is on its way: the end of the list is not announced while it is loaded
        rule.onNodeWithText(label(R.string.end_of_list)).assertDoesNotExist()

        gate.complete(Unit)

        scrollTo(fourthRepo.name)
        scrollTo(label(R.string.end_of_list))
    }

    // endregion

    // region failures

    @Test
    fun failedRefreshIsReportedWithAWayToRetryIt() {
        var offline = true
        val repository = FakeRepository().apply {
            load = { _, _ ->
                if (offline) PagingSource.LoadResult.Error(IOException("offline")) else page(REPOS)
            }
        }

        showScreen(repository = repository)
        scrollTo(label(R.string.error_refresh))

        offline = false
        rule.onNodeWithContentDescription(label(R.string.retry), useUnmergedTree = true)
            .performClick()

        scrollTo(FIRST_REPO.name)
        assertEquals(1, repository.queries.size)
    }

    @Test
    fun failedRefreshKeepsTheResultsOnScreen() {
        var offline = false
        val repository = FakeRepository().apply {
            load = { _, _ ->
                if (offline) PagingSource.LoadResult.Error(IOException("offline")) else page(REPOS)
            }
        }

        showScreen(search = FakeSearchResults(reply = DEFAULT_QUERY.value), repository = repository)
        scrollTo(FIRST_REPO.name)

        offline = true
        scrollTo(DEFAULT_QUERY.value)
        rule.onNodeWithText(DEFAULT_QUERY.value).performClick()

        rule.awaitCondition { repository.loads.size == 2 }
        rule.waitForIdle()

        // the results stay where they were, the failure is reported after the last of them
        scrollTo(FIRST_REPO.name)
        scrollTo(label(R.string.error_refresh))
        assertEquals(listOf(DEFAULT_QUERY), repository.queries)
    }

    @Test
    fun failedAppendKeepsTheLoadedResults() {
        val repository = FakeRepository().apply {
            load = { _, params ->
                if (params is PagingSource.LoadParams.Append) {
                    PagingSource.LoadResult.Error(IOException("offline"))
                } else {
                    page(REPOS, nextKey = 1)
                }
            }
        }

        showScreen(repository = repository)
        scrollTo(FIRST_REPO.name)
        awaitAppend()

        scrollTo(label(R.string.error_append))
        scrollTo(FIRST_REPO.name)
        assertEquals(1, repository.queries.size)
    }

    // endregion

    // region navigation

    @Test
    fun resultOpensItsRepo() {
        showScreen()
        scrollTo(FIRST_REPO.name)

        rule.onNodeWithText(FIRST_REPO.name).performClick()

        // opening the result is an effect of the ViewModel: it is performed once the screen
        // collects it, right after the click
        rule.awaitCondition { context.startedIntents.size == 1 }
        val intent = context.startedIntents.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(FIRST_REPO.url, intent.data?.toString())
    }

    @Test
    fun resultThatCannotBeOpenedKeepsTheScreenAlive() {
        // a watch without a browser answers the link with an activity only the system may start
        showScreen(startActivityError = SecurityException("no browser"))
        scrollTo(FIRST_REPO.name)

        rule.onNodeWithText(FIRST_REPO.name).performClick()

        // the link is handed over to nobody, and the screen stays on its results instead of dying
        // with the effect that failed to open it
        rule.awaitCondition { context.startedIntents.size == 1 }
        rule.onNodeWithText(FIRST_REPO.name).assertIsDisplayed()
    }

    // endregion

    // region search

    @Test
    fun searchAsksTheWatchForAQuery() {
        val search = FakeSearchResults(reply = "kotlin")

        showScreen(search = search)
        rule.onNodeWithText(DEFAULT_QUERY.value).performClick()

        assertEquals(label(R.string.search_label), search.inputs.single())
    }

    @Test
    fun customQueryReplacesTheResults() {
        val kotlinRepo = repo(id = 10, name = "kotlin-repo")
        val repository = FakeRepository().apply {
            load = { query, _ -> page(if (query == "kotlin") listOf(kotlinRepo) else REPOS) }
        }

        showScreen(search = FakeSearchResults(reply = "kotlin"), repository = repository)
        rule.onNodeWithText(DEFAULT_QUERY.value).performClick()

        rule.awaitCondition { repository.queries == listOf(DEFAULT_QUERY, query("kotlin")) }
        scrollTo("kotlin")
        scrollTo(kotlinRepo.name)

        // the results of the previous query are not kept next to the new ones
        assertEquals(0, rule.onAllNodes(hasText(FIRST_REPO.name)).fetchSemanticsNodes().size)
    }

    @Test
    fun sameQueryRefreshesTheResultsInsteadOfSearchingAgain() {
        val repository = FakeRepository()

        showScreen(search = FakeSearchResults(reply = DEFAULT_QUERY.value), repository = repository)
        scrollTo(FIRST_REPO.name)
        scrollTo(DEFAULT_QUERY.value)

        rule.onNodeWithText(DEFAULT_QUERY.value).performClick()

        rule.awaitCondition { repository.loads.size == 2 }
        assertEquals(listOf(DEFAULT_QUERY), repository.queries)

        // the results the screen had stay on screen while they are refreshed
        scrollTo(FIRST_REPO.name)
    }

    @Test
    fun blankQueryIsIgnored() {
        val repository = FakeRepository()

        showScreen(search = FakeSearchResults(reply = "   "), repository = repository)
        scrollTo(FIRST_REPO.name)
        scrollTo(DEFAULT_QUERY.value)

        rule.onNodeWithText(DEFAULT_QUERY.value).performClick()
        rule.waitForIdle()

        assertEquals(listOf(DEFAULT_QUERY), repository.queries)
        assertEquals(1, repository.loads.size)
    }

    @Test
    fun cancelledSearchKeepsTheResults() {
        val repository = FakeRepository()

        // the default reply of the search is a cancelled one, like a panel the user closed
        showScreen(repository = repository)
        scrollTo(FIRST_REPO.name)
        scrollTo(DEFAULT_QUERY.value)

        rule.onNodeWithText(DEFAULT_QUERY.value).performClick()
        rule.waitForIdle()

        assertEquals(listOf(DEFAULT_QUERY), repository.queries)
        assertEquals(1, repository.loads.size)
    }

    // endregion

    // region query state

    @Test
    fun newQueryStartsTheListFromTheTop() {
        showScreen()

        // the list is scrolled away from its head
        scrollTo(THIRD_REPO.name)

        rule.runOnUiThread { viewModel.onIntent(MainIntent.Search(query("kotlin"))) }
        rule.awaitCondition { viewModel.state.value.lastQueryScrolled == query("kotlin") }

        rule.onNodeWithText("kotlin").assertIsDisplayed()
    }

    @Test
    fun backRestoresTheDefaultQuery() {
        showScreen()
        scrollTo(FIRST_REPO.name)

        rule.runOnUiThread { viewModel.onIntent(MainIntent.Search(query("kotlin"))) }
        rule.awaitCondition { viewModel.state.value.query == query("kotlin") }
        rule.waitForIdle()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.awaitCondition { viewModel.state.value.lastQueryScrolled == DEFAULT_QUERY }

        rule.onNodeWithText(DEFAULT_QUERY.value).assertIsDisplayed()
        // the default query is shown from the cache, it is not asked for again
        assertEquals(listOf(DEFAULT_QUERY, query("kotlin")), repository.queries)
    }

    @Test
    fun visitedQueryKeepsItsResultsReady() {
        showScreen()
        rule.awaitCondition { repository.queries.size == 1 }

        search("kotlin")
        search("java")
        search("kotlin")

        assertEquals(1, repository.queries.count { it == query("kotlin") })
    }

    @Test
    fun leastRecentlyUsedQueryIsLoadedAgain() {
        showScreen()
        rule.awaitCondition { repository.queries.size == 1 }

        listOf("kotlin", "java", "swift", "rust").forEach(::search)
        search("kotlin")

        // the cache keeps the last visited queries only, the default one and kotlin were dropped
        assertEquals(2, repository.queries.count { it == query("kotlin") })
    }

    // endregion

    /** Shows the screen over the given data layer and search input. */
    private fun showScreen(
        search: FakeSearchResults = FakeSearchResults(),
        repository: FakeRepository = FakeRepository(),
        startActivityError: RuntimeException? = null
    ) {
        this.repository = repository
        viewModel = MainViewModel(GetReposFlowUseCase(repository), SavedStateHandle())
        context = RecordingContext(rule.activity, startActivityError)

        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalActivityResultRegistryOwner provides search
            ) {
                MaterialTheme {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    /** Scrolls the list to the given text, once the query has something to show. */
    private fun scrollTo(text: String) {
        rule.awaitCondition { repository.pagesServed > 0 }
        rule.waitForIdle()

        rule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
        rule.awaitNode(hasText(text))
    }

    /** Waits until the next page is requested. */
    private fun awaitAppend() {
        rule.awaitCondition { repository.loads.any { it is PagingSource.LoadParams.Append } }
        rule.waitForIdle()
    }

    /** Makes the screen search for the query the way the search panel does. */
    private fun search(rawQuery: String) {
        val query = SearchQuery.of(rawQuery)
        rule.runOnUiThread { viewModel.onIntent(MainIntent.Search(query)) }
        rule.awaitCondition { viewModel.state.value.query == query }
        rule.waitForIdle()
    }

    /** Builds the query the way the screen does: anything that comes in is canonical. */
    private fun query(raw: String): SearchQuery = SearchQuery.of(raw)

    /** The label of the star band with the given cap, e.g. `500K stars`. */
    private fun bandLabel(cap: Int): String = label(R.string.stars_label, CountFormatter.format(cap))
}
