package io.github.alxiw.wearpagingmediator.presentation.model

/**
 * The star bands the results of a search are grouped by: a band gets one label above its first
 * result, see [separatorAbove].
 */
object StarBand {

    /** The narrowest band the [stars] of a result reach, `null` for a result without stars. */
    fun of(stars: Int): Int? = THRESHOLDS.firstOrNull { stars >= it }

    /**
     * The band a separator is inserted for, `null` when the [after] result stays in the band of the
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
