package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the rules of the screen without a watch: a repeat of the query refreshes what is on
 * screen, a new one starts from the top, a tapped result opens its topics and a long-pressed one
 * is a one-off effect.
 *
 * The query of an intent is canonical by its type, so the tests build it the way the screen does.
 */
class MainReducerTest {

    private val state = MainState(
        query = query("android"),
        lastQueryScrolled = query("android"),
        scrollToTopPending = false
    )

    @Test
    fun newQueryStartsTheResultsFromTheTop() {
        val reduction = MainReducer.reduce(state, MainIntent.Search(query("kotlin")))

        assertEquals(
            MainState(
                query = query("kotlin"),
                lastQueryScrolled = query("android"),
                scrollToTopPending = true
            ),
            reduction.state
        )
        assertEquals(emptyList<MainEffect>(), reduction.effects)
    }

    @Test
    fun queryTheResultsAreAlreadyAtTheTopOfNeedsNoScroll() {
        val scrolled = MainState(query = query("kotlin"), lastQueryScrolled = query("kotlin"))

        val reduction = MainReducer.reduce(scrolled, MainIntent.Search(query("kotlin")))

        assertEquals(scrolled, reduction.state)
    }

    @Test
    fun submittedQueryIsTrimmed() {
        val reduction = MainReducer.reduce(state, MainIntent.QuerySubmitted("  kotlin \n"))

        assertEquals(query("kotlin"), reduction.state.query)
        assertEquals(true, reduction.state.scrollToTopPending)
    }

    @Test
    fun submittedMultiWordQueryIsSingleSpaced() {
        val reduction = MainReducer.reduce(state, MainIntent.QuerySubmitted("  Android \t kotlin \n"))

        assertEquals(query("android kotlin"), reduction.state.query)
        assertEquals(true, reduction.state.scrollToTopPending)
    }

    @Test
    fun closedPanelIsNotASearch() {
        assertEquals(Reduction(state), MainReducer.reduce(state, MainIntent.QuerySubmitted(null)))
    }

    @Test
    fun blankQueryIsNotASearch() {
        assertEquals(Reduction(state), MainReducer.reduce(state, MainIntent.QuerySubmitted("   ")))
    }

    @Test
    fun repeatedQueryInAnotherCaseOnlyRefreshesTheResults() {
        val reduction = MainReducer.reduce(state, MainIntent.QuerySubmitted("Android"))

        assertEquals(state, reduction.state)
        assertEquals(listOf(MainEffect.RefreshResults), reduction.effects)
    }

    @Test
    fun completedScrollClearsThePendingScroll() {
        val pending = state.copy(query = query("kotlin"), scrollToTopPending = true)

        val reduction = MainReducer.reduce(pending, MainIntent.ScrolledToTop(query("kotlin")))

        assertEquals(
            pending.copy(lastQueryScrolled = query("kotlin"), scrollToTopPending = false),
            reduction.state
        )
    }

    @Test
    fun completedScrollOfAnOlderQueryStillLeavesTheScrollPending() {
        val pending = state.copy(query = query("kotlin"), scrollToTopPending = true)

        val reduction = MainReducer.reduce(pending, MainIntent.ScrolledToTop(query("Android")))

        assertEquals(
            pending.copy(lastQueryScrolled = query("android"), scrollToTopPending = true),
            reduction.state
        )
    }

    @Test
    fun tappedRepoOpensItsTopicsDialog() {
        val repo = repo(id = 2, name = "paging", description = "A paging sample")

        val reduction = MainReducer.reduce(state, MainIntent.RepoClicked(repo))

        assertEquals(
            state.copy(
                topicsDialog = TopicsDialogState.Loading(repo.id, repo.name, repo.description)
            ),
            reduction.state
        )
        assertEquals(emptyList<MainEffect>(), reduction.effects)
    }

    @Test
    fun tappedRepoWithoutDescriptionOpensTheDialogWithoutIt() {
        val repo = repo(id = 2, name = "paging")

        val reduction = MainReducer.reduce(state, MainIntent.RepoClicked(repo))

        assertNull(reduction.state.topicsDialog?.repoDescription)
    }

    @Test
    fun longPressedRepoIsAnEffect() {
        val repo = repo(id = 3, name = "compose")

        val reduction = MainReducer.reduce(state, MainIntent.RepoLongClicked(repo))

        assertEquals(state, reduction.state)
        assertEquals(listOf(MainEffect.OpenUrl(repo.url)), reduction.effects)
    }

    @Test
    fun loadedTopicsFillTheOpenDialog() {
        val open = state.copy(topicsDialog = TopicsDialogState.Loading(1, "compose", DESCRIPTION))

        val reduction = MainReducer.reduce(
            open,
            MainIntent.TopicsLoaded(1, listOf("android", "paging"))
        )

        assertEquals(
            open.copy(
                topicsDialog = TopicsDialogState.Loaded(
                    repoId = 1,
                    repoName = "compose",
                    repoDescription = DESCRIPTION,
                    topics = listOf("android", "paging")
                )
            ),
            reduction.state
        )
    }

    @Test
    fun topicsOfAnotherRepoDoNotReplaceTheOpenDialog() {
        val open = state.copy(topicsDialog = TopicsDialogState.Loading(1, "compose", DESCRIPTION))

        // the load of a dialog that was closed or replaced with another repo is still on its way
        assertEquals(
            Reduction(open),
            MainReducer.reduce(open, MainIntent.TopicsLoaded(2, listOf("android")))
        )
        assertEquals(Reduction(open), MainReducer.reduce(open, MainIntent.TopicsLoadFailed(2)))
    }

    @Test
    fun failedTopicsAreReportedInTheOpenDialog() {
        val open = state.copy(topicsDialog = TopicsDialogState.Loading(1, "compose", DESCRIPTION))

        val reduction = MainReducer.reduce(open, MainIntent.TopicsLoadFailed(1))

        assertEquals(
            open.copy(topicsDialog = TopicsDialogState.Failed(1, "compose", DESCRIPTION)),
            reduction.state
        )
    }

    @Test
    fun dismissedTopicsDialogIsClosed() {
        val open = state.copy(
            topicsDialog = TopicsDialogState.Loaded(
                repoId = 1,
                repoName = "compose",
                repoDescription = DESCRIPTION,
                topics = listOf("android")
            )
        )

        val reduction = MainReducer.reduce(open, MainIntent.TopicsDialogDismissed)

        assertEquals(state, reduction.state)
        assertEquals(emptyList<MainEffect>(), reduction.effects)
    }

    @Test
    fun tappedTopicClosesTheDialogAndSearchesForIt() {
        val open = state.copy(
            topicsDialog = TopicsDialogState.Loaded(
                repoId = 1,
                repoName = "compose",
                repoDescription = DESCRIPTION,
                topics = listOf("kotlin")
            )
        )

        val reduction = MainReducer.reduce(open, MainIntent.TopicClicked(" Kotlin "))

        assertEquals(
            MainState(
                query = query("kotlin"),
                lastQueryScrolled = query("android"),
                scrollToTopPending = true
            ),
            reduction.state
        )
        assertEquals(emptyList<MainEffect>(), reduction.effects)
    }

    @Test
    fun tappedTopicThatIsAlreadySearchedOnlyRefreshesTheResults() {
        val open = state.copy(
            topicsDialog = TopicsDialogState.Loaded(
                repoId = 1,
                repoName = "compose",
                repoDescription = DESCRIPTION,
                topics = listOf("android")
            )
        )

        val reduction = MainReducer.reduce(open, MainIntent.TopicClicked("Android"))

        // the dialog is closed, but the results of the query stay on screen while they refresh
        assertEquals(state, reduction.state)
        assertEquals(listOf(MainEffect.RefreshResults), reduction.effects)
    }

    /** Builds the query the way the screen does: anything that comes in is canonical. */
    private fun query(raw: String): SearchQuery = SearchQuery.of(raw)

    /** A result of a search, the dialog of its topics needs its id, name and description. */
    private fun repo(id: Long, name: String, description: String? = null) = Repo(
        id = id,
        name = name,
        description = description,
        url = "https://github.com/alxiw/$name",
        stars = 1,
        forks = 1,
        language = null
    )

    private companion object {
        /** The description the dialog of the topics shows under the name of the repo. */
        const val DESCRIPTION = "A compose sample"
    }
}
