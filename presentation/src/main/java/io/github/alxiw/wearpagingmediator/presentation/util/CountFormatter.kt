package io.github.alxiw.wearpagingmediator.presentation.util

/**
 * Formats the counts of a card and the star band labels of the separators into one abbreviated
 * form, so that a result with 468224 stars reads "468K" both on its card and in the label above
 * its band.
 *
 * The counts are abbreviated with the K and M suffixes: the fractional part is dropped and a count
 * below 1000 stays as is. The suffixes are the locale independent ones GitHub itself uses, the
 * surrounding text (e.g. "≤ %1$s stars") stays localizable in the string resources.
 *
 * The dialog of the topics shows the counts in full instead, see [formatFull].
 */
object CountFormatter {

    /** The abbreviated form of [count] for a card or a star band label. */
    fun format(count: Int): String = when {
        count >= M -> "${count / M}M"
        count >= K -> "${count / K}K"
        else -> count.toString()
    }

    /**
     * The exact count for the dialog of the topics: nothing is dropped, so a result with 468224
     * stars reads "468224" there and the real number behind the "468K" of its card is visible.
     * No grouping separators are added: they would make the number wider on a watch and the
     * abbreviated form of the cards has none either.
     */
    fun formatFull(count: Int): String = count.toString()

    private const val K = 1_000
    private const val M = 1_000_000
}
