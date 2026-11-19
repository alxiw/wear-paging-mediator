package io.github.alxiw.wearpagingmediator.presentation.model

/**
 * A one-off action the screen has to perform. An effect is not a part of [MainState]: it is
 * consumed once and never rendered again (e.g. on an unrelated recomposition).
 */
sealed interface MainEffect {

    /**
     * The current query was asked for again: only the network part is repeated, the results that
     * are on screen stay there while they are refreshed.
     */
    data object RefreshResults : MainEffect

    /** The user opened the result of a repo: show [url] outside of the app. */
    data class OpenUrl(val url: String) : MainEffect
}
