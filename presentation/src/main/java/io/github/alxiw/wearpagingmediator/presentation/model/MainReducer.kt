package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery

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
        is MainIntent.Search -> Reduction(state.searching(intent.query))
        is MainIntent.QuerySubmitted -> reduceSubmission(state, intent.query)
        is MainIntent.ScrolledToTop -> reduceScroll(state, intent.query)
        is MainIntent.RepoClicked -> Reduction(
            state.copy(
                topicsDialog = TopicsDialogState.Loading(
                    repoId = intent.repo.id,
                    repoName = intent.repo.name,
                    repoDescription = intent.repo.description,
                    repoStars = intent.repo.stars,
                    repoForks = intent.repo.forks
                )
            )
        )
        is MainIntent.RepoLongClicked -> Reduction(
            state = state,
            effects = listOf(MainEffect.OpenUrl(intent.repo.url))
        )
        is MainIntent.TopicsDialogDismissed -> Reduction(state.copy(topicsDialog = null))
        is MainIntent.TopicClicked -> reduceTopicClicked(state, intent.topic)
        is MainIntent.TopicsLoaded -> reduceTopicsLoaded(state, intent)
        is MainIntent.TopicsLoadFailed -> reduceTopicsFailed(state, intent)
    }

    /**
     * A closed panel and a blank reply are not a search. The same query is not a new search
     * either: the paging flow ignores it, so only the network part is repeated and the cached
     * results stay on screen while they are refreshed. The raw reply is turned into its canonical
     * form here, so `Android`, `android` and `  ANDROID ` are the same search.
     */
    private fun reduceSubmission(state: MainState, rawQuery: String?): Reduction {
        val query = rawQuery?.let(SearchQuery::of) ?: return Reduction(state)
        if (query.isEmpty) return Reduction(state)

        return when (query) {
            state.query -> Reduction(state, listOf(MainEffect.RefreshResults))
            else -> Reduction(state.searching(query))
        }
    }

    /**
     * A tapped topic is a search: the dialog that showed it is closed and the topic becomes the
     * query. It goes through the rules of a submitted search, so tapping the topic that is already
     * searched only refreshes the results that are on screen.
     */
    private fun reduceTopicClicked(state: MainState, topic: String): Reduction =
        reduceSubmission(state.copy(topicsDialog = null), topic)

    private fun reduceScroll(state: MainState, query: SearchQuery): Reduction = Reduction(
        state.copy(
            lastQueryScrolled = query,
            // a query that changed during the scroll still has to start from the top
            scrollToTopPending = state.query != query
        )
    )

    /** A result of a load is applied only to the dialog it was started for. */
    private fun reduceTopicsLoaded(
        state: MainState,
        intent: MainIntent.TopicsLoaded
    ): Reduction {
        val dialog = state.topicsDialog
        if (dialog?.repoId != intent.repoId) return Reduction(state)
        return Reduction(
            state.copy(
                topicsDialog = TopicsDialogState.Loaded(
                    repoId = intent.repoId,
                    repoName = dialog.repoName,
                    repoDescription = dialog.repoDescription,
                    repoStars = dialog.repoStars,
                    repoForks = dialog.repoForks,
                    topics = intent.topics
                )
            )
        )
    }

    private fun reduceTopicsFailed(
        state: MainState,
        intent: MainIntent.TopicsLoadFailed
    ): Reduction {
        val dialog = state.topicsDialog
        if (dialog?.repoId != intent.repoId) return Reduction(state)
        return Reduction(
            state.copy(
                topicsDialog = TopicsDialogState.Failed(
                    repoId = intent.repoId,
                    repoName = dialog.repoName,
                    repoDescription = dialog.repoDescription,
                    repoStars = dialog.repoStars,
                    repoForks = dialog.repoForks
                )
            )
        )
    }

    /** A new query always starts the results from the top, unless they already are there. */
    private fun MainState.searching(query: SearchQuery): MainState = copy(
        query = query,
        scrollToTopPending = query != lastQueryScrolled
    )
}
