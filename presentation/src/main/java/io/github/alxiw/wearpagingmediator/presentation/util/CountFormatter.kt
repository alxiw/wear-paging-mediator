package io.github.alxiw.wearpagingmediator.presentation.util

/**
 * Formats the counts of a card and the star band labels of the separators into one abbreviated
 * form, so that a result with 468224 stars reads "468K" both on its card and in the label above
 * its band.
 *
 * The counts are abbreviated with the K and M suffixes: the fractional part is dropped and a count
 * below 1000 stays as is. The suffixes are the locale independent ones GitHub itself uses, the
 * surrounding text (e.g. "≤ %1$s stars") stays localizable in the string resources.
 */
object CountFormatter {

    fun format(count: Int): String = when {
        count >= M -> "${count / M}M"
        count >= K -> "${count / K}K"
        else -> count.toString()
    }

    private const val K = 1_000
    private const val M = 1_000_000
}
