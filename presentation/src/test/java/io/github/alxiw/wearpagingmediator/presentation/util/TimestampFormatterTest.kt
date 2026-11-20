package io.github.alxiw.wearpagingmediator.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

/**
 * Guards the moment of a recent query, see [TimestampFormatter]: the time comes before the date,
 * and the moment is shown in the zone it is formatted for.
 */
class TimestampFormatterTest {

    private val instant = Instant.parse("2026-10-21T17:16:56Z")

    @Test
    fun timeComesBeforeTheDate() {
        assertEquals("17:16:56 21.10.2026", TimestampFormatter.format(instant, ZoneOffset.UTC))
    }

    @Test
    fun momentIsShownInTheGivenZone() {
        assertEquals(
            "20:16:56 21.10.2026",
            TimestampFormatter.format(instant, ZoneOffset.ofHours(3))
        )
    }

    @Test
    fun momentOfAnotherDayIsShownWithItsOwnDate() {
        assertEquals(
            "00:30:00 01.01.2026",
            TimestampFormatter.format(Instant.parse("2025-12-31T23:30:00Z"), ZoneOffset.ofHours(1))
        )
    }
}
