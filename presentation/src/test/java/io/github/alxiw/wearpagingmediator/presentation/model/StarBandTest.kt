package io.github.alxiw.wearpagingmediator.presentation.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Guards the star band labels of the results, see [StarBand].
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
        assertEquals(50_000, StarBand.separatorAbove(before = 468_000, after = 90_000))
        assertEquals(500, StarBand.separatorAbove(before = 90_000, after = 900))
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
    fun bandIsTheNarrowestThresholdTheResultReaches() {
        assertEquals(1_000_000, StarBand.of(2_500_000))
        assertEquals(500_000, StarBand.of(999_999))
        assertEquals(100_000, StarBand.of(100_000))
        assertEquals(1, StarBand.of(1))
    }
}
