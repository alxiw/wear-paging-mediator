package io.github.alxiw.wearpagingmediator.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the star band labels of the results, see [StarBand].
 *
 * A band is capped by its threshold: the label "100 and below" covers the results with 100 down to
 * 51 stars, the next label "50 and below" covers 50 down to 11 stars and so on.
 *
 * The first group of a search stays unlabelled on purpose: its label arrives after the list has
 * been laid out and pushes the search button off the screen, see [StarBand.separatorAbove].
 */
class StarBandTest {

    @Test
    fun firstGroupOfTheListIsNotLabelled() {
        assertNull(StarBand.separatorAbove(before = null, after = 468_000))
        assertNull(StarBand.separatorAbove(before = null, after = 1))
        assertNull(StarBand.separatorAbove(before = null, after = 0))
    }

    @Test
    fun narrowerBandOfTheNextResultIsLabelled() {
        assertEquals(100_000, StarBand.separatorAbove(before = 468_000, after = 90_000))
        assertEquals(1_000, StarBand.separatorAbove(before = 90_000, after = 900))
    }

    @Test
    fun separatorCapsTheResultsBelowIt() {
        // results with 100..51 stars stay under the label of the 100 band
        assertNull(StarBand.separatorAbove(before = 100, after = 51))
        assertNull(StarBand.separatorAbove(before = 51, after = 51))
        assertEquals(50, StarBand.separatorAbove(before = 51, after = 50))
        // the first result under the cap of 100 gets the label of its band
        assertEquals(100, StarBand.separatorAbove(before = 101, after = 100))
    }

    @Test
    fun descendingResultsAreGroupedByTheCapOfEachBand() {
        val stars = listOf(468_000, 177_000, 101, 100, 99, 51, 50, 11, 10, 6, 5, 1, 0)
        val labels = stars.zipWithNext { before, after ->
            StarBand.separatorAbove(before = before, after = after)
        }
        assertEquals(listOf(500, 100, 50, 10, 5, 1), labels.filterNotNull())
    }

    @Test
    fun resultsOfTheSameBandShareOneLabel() {
        assertNull(StarBand.separatorAbove(before = 468_000, after = 181_000))
        assertNull(StarBand.separatorAbove(before = 5, after = 5))
    }

    @Test
    fun widerBandOfTheNextResultAddsNoLabel() {
        // an unsorted query must not repeat a label for a band that has already been passed
        assertNull(StarBand.separatorAbove(before = 900, after = 90_000))
        assertNull(StarBand.separatorAbove(before = 5, after = 1_000_000))
    }

    @Test
    fun resultWithoutStarsIsNotLabelled() {
        assertNull(StarBand.separatorAbove(before = 468_000, after = 0))
        assertNull(StarBand.of(0))
    }

    @Test
    fun bandIsTheNextThresholdAtOrAboveTheResult() {
        assertEquals(1_000_000, StarBand.of(2_500_000))
        assertEquals(1_000_000, StarBand.of(999_999))
        assertEquals(100_000, StarBand.of(100_000))
        assertEquals(100, StarBand.of(99))
        assertEquals(100, StarBand.of(51))
        assertEquals(50, StarBand.of(50))
        assertEquals(1, StarBand.of(1))
    }
}
