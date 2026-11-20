package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the rules of the screen without a watch: a repeat of the query refreshes what is on
 * screen, a new one starts from the top and an opened result is a one-off effect.
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
    fun openedRepoIsAnEffect() {
        val repo = Repo(
            id = 1,
            name = "compose",
            description = null,
            url = "https://github.com/alxiw/compose",
            stars = 1,
            forks = 1,
            language = null
        )

        val reduction = MainReducer.reduce(state, MainIntent.RepoClicked(repo))

        assertEquals(state, reduction.state)
        assertEquals(listOf(MainEffect.OpenUrl(repo.url)), reduction.effects)
    }

    /** Builds the query the way the screen does: anything that comes in is canonical. */
    private fun query(raw: String): SearchQuery = SearchQuery.of(raw)
}
