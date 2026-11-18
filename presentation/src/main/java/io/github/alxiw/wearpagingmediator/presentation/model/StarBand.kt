package io.github.alxiw.wearpagingmediator.presentation.model

/**
 * The star bands the results of a search are grouped by: a band gets one label above its first
 * result, see [separatorAbove].
 *
 * A band is capped by its threshold: the label "100 and below" covers the results with 100 down to
 * 51 stars, the next label "50 and below" covers 50 down to 11 stars and so on.
 */
object StarBand {

    /**
     * The cap of the band the [stars] of a result fall under: the smallest threshold that is not
     * less than [stars], `null` for a result without stars.
     */
    fun of(stars: Int): Int? = when {
        stars <= 0 -> null
        // results above the largest threshold stay in the open top band that is never labelled
        else -> THRESHOLDS.lastOrNull { stars <= it } ?: THRESHOLDS.first()
    }

    /**
     * The cap of the band a separator is inserted for, `null` when the [after] result stays in the
     * band of the [before] one.
     *
     * The bands are ordered from the widest cap to the narrowest one as the results go down, so a
     * separator appears only when the cap of the [after] result is narrower than the cap of the
     * [before] one.
     *
     * The first result of the list is never labelled ([before] is `null`): such a label is inserted
     * above the card that the list anchors in the middle of the screen, and
     * `TransformingLazyColumn` compensates the insertion by moving everything above the card up -
     * the search button left the screen even though the list stood at its very top.
     */
    fun separatorAbove(before: Int?, after: Int): Int? {
        val afterBand = of(after) ?: return null
        val beforeBand = before?.let(::of) ?: return null
        return afterBand.takeIf { beforeBand > it }
    }

    private val THRESHOLDS = listOf(
        1_000_000, 500_000, 100_000, 50_000, 10_000, 5_000, 1_000, 500, 100, 50, 10, 5, 1
    )
}
