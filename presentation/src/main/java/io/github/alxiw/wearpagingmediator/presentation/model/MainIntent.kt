package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.Repo

/**
 * Everything the user can ask the screen to do. An intent is the only way the UI changes
 * [MainState]: the screen only turns the events of the watch into intents and renders the state
 * that comes back.
 */
sealed interface MainIntent {

    /**
     * The user chose [query] as the query of the results: a new search, a repeated one or the
     * reset back to the default query done by back.
     */
    data class Search(val query: String) : MainIntent

    /**
     * The voice panel returned [query]: the raw text of the reply, `null` when the panel was
     * closed without a reply. The query is trimmed and validated by the reducer.
     */
    data class QuerySubmitted(val query: String?) : MainIntent

    /** The list has been scrolled back to the top for [query]. */
    data class ScrolledToTop(val query: String) : MainIntent

    /** The user opened the result of [repo]. */
    data class RepoClicked(val repo: Repo) : MainIntent
}
