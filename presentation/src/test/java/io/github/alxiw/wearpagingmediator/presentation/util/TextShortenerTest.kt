package io.github.alxiw.wearpagingmediator.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Guards what a card lays out, see [TextShortener].
 *
 * A description of a result is not bounded by GitHub and can be tens of thousands of characters
 * long: laying such a text out in full takes seconds on a watch, so a card shows only its head.
 */
class TextShortenerTest {

    @Test
    fun missingDescriptionStaysEmpty() {
        assertEquals("", TextShortener.shorten(null))
        assertEquals("", TextShortener.shorten(""))
    }

    @Test
    fun textThatFitsTheCardStaysAsIs() {
        val description = "The first result of the search"
        assertEquals(description, TextShortener.shorten(description))

        val atTheLimit = "x".repeat(TextShortener.CARD_TEXT_LIMIT)
        assertEquals(atTheLimit, TextShortener.shorten(atTheLimit))
    }

    @Test
    fun textLongerThanTheCardShowsIsCutToItsLimit() {
        // the real one: a description of 55700 characters, 33900 of them ideographic spaces
        val unbound = "\u3000".repeat(33_900) + "PDF ".repeat(5_450)
        val shortened = TextShortener.shorten(unbound)

        assertEquals(TextShortener.CARD_TEXT_LIMIT, shortened.length)
        assertEquals(unbound.take(TextShortener.CARD_TEXT_LIMIT), shortened)
    }

    @Test
    fun aSurrogatePairIsNotCutInHalf() {
        val emoji = "\uD83D\uDE00" // 😀, two chars
        // the limit falls between the halves of the emoji
        val unbound = "x".repeat(TextShortener.CARD_TEXT_LIMIT - 1) + emoji + "tail"
        val shortened = TextShortener.shorten(unbound)

        assertEquals("x".repeat(TextShortener.CARD_TEXT_LIMIT - 1), shortened)
        assertFalse(shortened.last().isHighSurrogate())
    }
}
