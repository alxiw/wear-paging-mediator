package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.RecentQuery
import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo

/**
 * Everything the user can ask the screen to do. An intent is the only way the UI changes
 * [MainState]: the screen only turns the events of the watch into intents and renders the state
 * that comes back.
 */
sealed interface MainIntent {

    /**
     * The user chose [query] as the query of the results: a new search, a repeated one or the reset
     * to the default one done by back.
     */
    data class Search(val query: SearchQuery) : MainIntent

    /** The reply of the voice panel: raw text, `null` when it was closed without a reply. */
    data class QuerySubmitted(val query: String?) : MainIntent

    /** The list has been scrolled back to the top for [query]. */
    data class ScrolledToTop(val query: SearchQuery) : MainIntent

    /** The user tapped the result of [repo]: show its topics. */
    data class RepoClicked(val repo: Repo) : MainIntent

    /** The user long-pressed the result of [repo]: open its page. */
    data class RepoLongClicked(val repo: Repo) : MainIntent

    /** The user dismissed the topics dialog. */
    data object TopicsDialogDismissed : MainIntent

    /**
     * The user tapped [topic]: the dialog is closed and the raw text becomes the query, exactly as
     * if the user had typed and submitted it.
     */
    data class TopicClicked(val topic: String) : MainIntent

    /** The topics of [repoId] were read, the result of [RepoClicked] sent by the state holder. */
    data class TopicsLoaded(val repoId: Long, val topics: List<String>) : MainIntent

    /** The topics of [repoId] could not be read, the result of [RepoClicked]. */
    data class TopicsLoadFailed(val repoId: Long) : MainIntent

    /** The user long-pressed the search button: show the most recently used queries. */
    data object RecentQueriesRequested : MainIntent

    /** The user dismissed the dialog with the recent queries. */
    data object RecentQueriesDialogDismissed : MainIntent

    /**
     * The user tapped [query] in the dialog of the recent queries: the dialog is closed and the
     * query becomes the query of the results, as in [TopicClicked].
     */
    data class RecentQueryClicked(val query: String) : MainIntent

    /** The recent queries were read, the result of [RecentQueriesRequested]. */
    data class RecentQueriesLoaded(val queries: List<RecentQuery>) : MainIntent

    /** The recent queries could not be read, the result of [RecentQueriesRequested]. */
    data object RecentQueriesLoadFailed : MainIntent
}
