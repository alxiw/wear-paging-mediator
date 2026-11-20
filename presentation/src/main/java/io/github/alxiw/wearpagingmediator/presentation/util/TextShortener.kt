package io.github.alxiw.wearpagingmediator.presentation.util

/**
 * Shortens the texts of a card to what the card can show.
 *
 * GitHub does not bound a description: one result of a search can carry tens of thousands of
 * characters. A `maxLines` does not help there - the platform lays the whole paragraph out and only
 * clips the drawing - so such a text is measured in full on every measure of its card, and a card
 * measured on the main thread (scrolling, prefetching the next item) blocks the frames and the
 * input for seconds. The cut text is still shown with the ellipsis of the card.
 *
 * The counts and the names of a repository are bounded by GitHub, so only a description needs it.
 */
object TextShortener {

    /** How many characters of a text a card can show, the rest stays behind its ellipsis. */
    const val CARD_TEXT_LIMIT = 300

    fun shorten(text: String?): String {
        if (text == null || text.length <= CARD_TEXT_LIMIT) return text.orEmpty()
        // a surrogate pair is two chars: cutting it in half would leave an unpaired surrogate
        val end = if (text[CARD_TEXT_LIMIT - 1].isHighSurrogate()) CARD_TEXT_LIMIT - 1 else CARD_TEXT_LIMIT
        return text.substring(0, end)
    }
}
