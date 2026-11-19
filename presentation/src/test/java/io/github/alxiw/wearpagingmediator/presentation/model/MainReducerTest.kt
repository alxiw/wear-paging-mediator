package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.Repo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the rules of the screen without a watch: a repeat of the query refreshes what is on
 * screen, a new one starts from the top and an opened result is a one-off effect.
 */
class MainReducerTest {

    private val state = MainState(
        query = "Android",
        lastQueryScrolled = "Android",
        scrollToTopPending = false
    )

    @Test
    fun newQueryStartsTheResultsFromTheTop() {
        val reduction = MainReducer.reduce(state, MainIntent.Search("kotlin"))

        assertEquals(
            MainState(query = "kotlin", lastQueryScrolled = "Android", scrollToTopPending = true),
            reduction.state
        )
        assertEquals(emptyList<MainEffect>(), reduction.effects)
    }

    @Test
    fun queryTheResultsAreAlreadyAtTheTopOfNeedsNoScroll() {
        val scrolled = MainState(query = "kotlin", lastQueryScrolled = "kotlin")

        val reduction = MainReducer.reduce(scrolled, MainIntent.Search("kotlin"))

        assertEquals(scrolled, reduction.state)
    }

    @Test
    fun submittedQueryIsTrimmed() {
        val reduction = MainReducer.reduce(state, MainIntent.QuerySubmitted("  kotlin \n"))

        assertEquals("kotlin", reduction.state.query)
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
    fun repeatedQueryOnlyRefreshesTheResults() {
        val reduction = MainReducer.reduce(state, MainIntent.QuerySubmitted("Android"))

        assertEquals(state, reduction.state)
        assertEquals(listOf(MainEffect.RefreshResults), reduction.effects)
    }

    @Test
    fun completedScrollClearsThePendingScroll() {
        val pending = state.copy(query = "kotlin", scrollToTopPending = true)

        val reduction = MainReducer.reduce(pending, MainIntent.ScrolledToTop("kotlin"))

        assertEquals(
            pending.copy(lastQueryScrolled = "kotlin", scrollToTopPending = false),
            reduction.state
        )
    }

    @Test
    fun completedScrollOfAnOlderQueryStillLeavesTheScrollPending() {
        val pending = state.copy(query = "kotlin", scrollToTopPending = true)

        val reduction = MainReducer.reduce(pending, MainIntent.ScrolledToTop("Android"))

        assertEquals(
            pending.copy(lastQueryScrolled = "Android", scrollToTopPending = true),
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
}
