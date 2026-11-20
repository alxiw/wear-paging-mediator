package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.SearchQuery

/** The result of handling an intent: the new state and the one-off actions it asks for. */
internal data class Reduction(
    val state: MainState,
    val effects: List<MainEffect> = emptyList()
)

/**
 * The single place the state of the screen changes. The reducer is pure, so the rules of the
 * screen (what a repeat of the query does, when the list has to go back to the top) are guarded by
 * a JVM test instead of being spread over the composables.
 */
internal object MainReducer {

    fun reduce(state: MainState, intent: MainIntent): Reduction = when (intent) {
        is MainIntent.Search -> Reduction(state.searching(SearchQuery.normalize(intent.query)))
        is MainIntent.QuerySubmitted -> reduceSubmission(state, intent.query)
        is MainIntent.ScrolledToTop -> reduceScroll(state, SearchQuery.normalize(intent.query))
        is MainIntent.RepoClicked -> Reduction(
            state = state,
            effects = listOf(MainEffect.OpenUrl(intent.repo.url))
        )
    }

    /**
     * A closed panel and a blank reply are not a search. The same query is not a new search
     * either: the paging flow ignores it, so only the network part is repeated and the cached
     * results stay on screen while they are refreshed. The query is compared in its canonical
     * form, so `Android`, `android` and `  ANDROID ` are the same search.
     */
    private fun reduceSubmission(state: MainState, rawQuery: String?): Reduction {
        val query = rawQuery?.let { SearchQuery.normalize(it) }?.takeIf { it.isNotEmpty() }
        return when (query) {
            null -> Reduction(state)
            state.query -> Reduction(state, listOf(MainEffect.RefreshResults))
            else -> Reduction(state.searching(query))
        }
    }

    private fun reduceScroll(state: MainState, query: String): Reduction = Reduction(
        state.copy(
            lastQueryScrolled = query,
            // a query that changed during the scroll still has to start from the top
            scrollToTopPending = state.query != query
        )
    )

    /** A new query always starts the results from the top, unless they already are there. */
    private fun MainState.searching(query: String): MainState = copy(
        query = query,
        scrollToTopPending = query != lastQueryScrolled
    )
}
