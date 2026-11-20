package io.github.alxiw.wearpagingmediator.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the counts of the cards and the star band labels, see [CountFormatter].
 *
 * A card and the label above its band must read the same count: a result with 468224 stars is
 * "468K" both on the card and in "≤ 500K stars", not "468224" on one of them. The dialog of the
 * topics is the other way around: it shows the exact count, so the number behind the abbreviation
 * stays visible.
 */
class CountFormatterTest {

    @Test
    fun countBelowOneThousandStaysAsIs() {
        assertEquals("0", CountFormatter.format(0))
        assertEquals("1", CountFormatter.format(1))
        assertEquals("999", CountFormatter.format(999))
    }

    @Test
    fun thousandsAreAbbreviatedWithK() {
        assertEquals("1K", CountFormatter.format(1_000))
        assertEquals("5K", CountFormatter.format(5_000))
        assertEquals("468K", CountFormatter.format(468_224))
        assertEquals("999K", CountFormatter.format(999_999))
    }

    @Test
    fun millionsAreAbbreviatedWithM() {
        assertEquals("1M", CountFormatter.format(1_000_000))
        assertEquals("2M", CountFormatter.format(2_500_000))
    }

    @Test
    fun exactCountsAreKeptInFull() {
        assertEquals("0", CountFormatter.formatFull(0))
        assertEquals("999", CountFormatter.formatFull(999))
        assertEquals("1000", CountFormatter.formatFull(1_000))
        assertEquals("468224", CountFormatter.formatFull(468_224))
        assertEquals("2500000", CountFormatter.formatFull(2_500_000))
    }

    @Test
    fun separatorCapsAreFormattedAsTheCards() {
        val caps = listOf(
            1_000_000, 500_000, 200_000, 100_000, 50_000, 20_000, 10_000, 5_000, 2_000, 1_000,
            500, 200, 100, 50, 20, 10, 5, 1
        )
        assertEquals(
            listOf(
                "1M", "500K", "200K", "100K", "50K", "20K", "10K", "5K", "2K", "1K",
                "500", "200", "100", "50", "20", "10", "5", "1"
            ),
            caps.map(CountFormatter::format)
        )
    }
}
