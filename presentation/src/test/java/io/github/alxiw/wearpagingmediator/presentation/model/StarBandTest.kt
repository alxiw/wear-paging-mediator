package io.github.alxiw.wearpagingmediator.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the star band labels of the results, see [StarBand].
 *
 * A separator at threshold `T` means every result above it has at least `T` stars (`>= T`) and
 * every result in the band below it has fewer than `T` stars (`< T`). For example, "50 stars"
 * sits between 50 and 49 and covers results with 49 down to 20 stars.
 *
 * The first group of a search stays unlabelled on purpose: its label arrives after the list has
 * been laid out and pushes the search button off the screen, see [StarBand.separatorAbove].
 */
class StarBandTest {

    @Test
    fun firstGroupOfTheListIsNotLabelled() {
        assertNull(StarBand.separatorAbove(before = null, after = 2_500_000))
        assertNull(StarBand.separatorAbove(before = null, after = 468_000))
        assertNull(StarBand.separatorAbove(before = null, after = 1))
        assertNull(StarBand.separatorAbove(before = null, after = 0))
    }

    @Test
    fun narrowerBandOfTheNextResultIsLabelled() {
        assertEquals(1_000_000, StarBand.separatorAbove(before = 1_000_000, after = 999_999))
        assertEquals(500_000, StarBand.separatorAbove(before = 1_200_000, after = 400_000))
        assertEquals(100_000, StarBand.separatorAbove(before = 468_000, after = 90_000))
        assertEquals(1_000, StarBand.separatorAbove(before = 90_000, after = 900))
        assertEquals(1, StarBand.separatorAbove(before = 468_000, after = 0))
    }

    @Test
    fun separatorSitsBelowResultsAtOrAboveItsThreshold() {
        // 52, 51, 50 stay above the "50 stars" separator; 49, 48 fall below it
        assertNull(StarBand.separatorAbove(before = 52, after = 51))
        assertNull(StarBand.separatorAbove(before = 51, after = 50))
        assertEquals(50, StarBand.separatorAbove(before = 50, after = 49))
        assertNull(StarBand.separatorAbove(before = 49, after = 48))
        // 20 stays in the < 50 band; 19 enters the < 20 band
        assertNull(StarBand.separatorAbove(before = 48, after = 20))
        assertEquals(20, StarBand.separatorAbove(before = 20, after = 19))
    }

    @Test
    fun allThresholdsCanAppearAsSeparators() {
        val stars = listOf(
            1_000_000, 999_999, 499_999, 199_999, 99_999, 49_999, 19_999, 9_999, 4_999, 1_999,
            999, 499, 199, 99, 49, 19, 9, 4, 0
        )
        val labels = stars.zipWithNext { before, after ->
            StarBand.separatorAbove(before = before, after = after)
        }
        assertEquals(
            listOf(
                1_000_000, 500_000, 200_000, 100_000, 50_000, 20_000, 10_000, 5_000, 2_000, 1_000,
                500, 200, 100, 50, 20, 10, 5, 1
            ),
            labels
        )
    }

    @Test
    fun resultsOfTheSameBandShareOneLabel() {
        assertNull(StarBand.separatorAbove(before = 2_500_000, after = 1_000_000))
        assertNull(StarBand.separatorAbove(before = 468_000, after = 200_000))
        assertNull(StarBand.separatorAbove(before = 5, after = 5))
        assertNull(StarBand.separatorAbove(before = 0, after = 0))
    }

    @Test
    fun widerBandOfTheNextResultAddsNoLabel() {
        // an unsorted query must not repeat a label for a band that has already been passed
        assertNull(StarBand.separatorAbove(before = 900, after = 90_000))
        assertNull(StarBand.separatorAbove(before = 5, after = 1_000_000))
        assertNull(StarBand.separatorAbove(before = 0, after = 1))
    }

    @Test
    fun negativeStarsAreNotLabelled() {
        assertNull(StarBand.of(-1))
        assertNull(StarBand.separatorAbove(before = 5, after = -1))
        assertNull(StarBand.separatorAbove(before = -1, after = 0))
    }

    @Test
    fun bandIsTheNextThresholdStrictlyAboveTheResult() {
        assertNull(StarBand.of(2_500_000))
        assertNull(StarBand.of(1_000_000))
        assertEquals(1_000_000, StarBand.of(999_999))
        assertEquals(1_000_000, StarBand.of(500_000))
        assertEquals(500_000, StarBand.of(499_999))
        assertEquals(200, StarBand.of(100))
        assertEquals(100, StarBand.of(99))
        assertEquals(100, StarBand.of(50))
        assertEquals(50, StarBand.of(49))
        assertEquals(50, StarBand.of(20))
        assertEquals(20, StarBand.of(19))
        assertEquals(5, StarBand.of(1))
        assertEquals(1, StarBand.of(0))
    }
}
