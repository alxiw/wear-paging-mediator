package io.github.alxiw.wearpagingmediator.presentation.model

import io.github.alxiw.wearpagingmediator.domain.model.SearchQuery
import io.github.alxiw.wearpagingmediator.domain.model.Repo

/**
 * Everything the user can ask the screen to do. An intent is the only way the UI changes
 * [MainState]: the screen only turns the events of the watch into intents and renders the state
 * that comes back.
 */
sealed interface MainIntent {

    /**
     * The user chose [query] as the query of the results: a new search, a repeated one or the
     * reset back to the default query done by back. The query is canonical by its type, the screen
     * builds it with `SearchQuery.of` at the edge where the text comes from.
     */
    data class Search(val query: SearchQuery) : MainIntent

    /**
     * The voice panel returned [query]: the raw text of the reply, `null` when the panel was
     * closed without a reply. The reducer turns it into the canonical query and rejects a blank
     * one, so this is the only place a raw string enters the state.
     */
    data class QuerySubmitted(val query: String?) : MainIntent

    /** The list has been scrolled back to the top for [query]. */
    data class ScrolledToTop(val query: SearchQuery) : MainIntent

    /** The user opened the result of [repo]. */
    data class RepoClicked(val repo: Repo) : MainIntent
}
