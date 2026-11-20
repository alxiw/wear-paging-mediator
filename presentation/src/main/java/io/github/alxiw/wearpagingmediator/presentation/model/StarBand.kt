package io.github.alxiw.wearpagingmediator.presentation.model

/**
 * The star bands the results of a search are grouped by: a band gets one label above its first
 * result, see [separatorAbove].
 *
 * A separator at threshold `T` means every result above it has at least `T` stars (`>= T`) and
 * every result in the band below it has fewer than `T` stars (`< T`). For example, the label
 * "50 stars" sits between 50 and 49 and covers results with 49 down to 20 stars; "1 star" sits
 * between 1 and 0 and covers results with 0 stars.
 */
object StarBand {

    /**
     * The exclusive upper bound of the band [stars] falls into: the smallest threshold strictly
     * greater than [stars], or `null` for the open top band (`stars >= 1_000_000`) and negative
     * values.
     */
    fun of(stars: Int): Int? = when {
        stars < 0 -> null
        else -> THRESHOLDS.lastOrNull { stars < it }
    }

    /**
     * The threshold of the separator inserted above [after], or `null` when [after] stays in the
     * same or a wider band than [before].
     *
     * The bands are ordered from the widest cap to the narrowest one as the results go down, so a
     * separator appears only when the band of [after] is strictly narrower than the band of
     * [before] (with the open top band `>= 1_000_000` being wider than every threshold).
     *
     * The first result of the list is never labelled ([before] is `null`): such a label is inserted
     * above the card that the list anchors in the middle of the screen, and
     * `TransformingLazyColumn` compensates the insertion by moving everything above the card up -
     * the search button left the screen even though the list stood at its very top.
     */
    fun separatorAbove(before: Int?, after: Int): Int? {
        if (before == null || before < 0) return null
        val afterBand = of(after) ?: return null
        val beforeBand = of(before)
        return afterBand.takeIf { beforeBand == null || beforeBand > it }
    }

    private val THRESHOLDS = listOf(
        1_000_000, 500_000, 200_000, 100_000, 50_000, 20_000, 10_000, 5_000, 2_000, 1_000,
        500, 200, 100, 50, 20, 10, 5, 1
    )
}
