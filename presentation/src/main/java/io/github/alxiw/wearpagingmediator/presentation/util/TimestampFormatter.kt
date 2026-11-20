package io.github.alxiw.wearpagingmediator.presentation.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats the moment a query was used for the dialog of the recent queries: the time comes first,
 * because the hour tells two searches made on the same day apart, the date follows it.
 *
 * The pattern is fixed and [Locale.ROOT] is used, so the separators and the digits do not depend on
 * the locale of the watch. The zone is a parameter, so the formatter is tested without depending on
 * the zone of the machine it runs on.
 */
object TimestampFormatter {

    private val FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss dd.MM.yyyy", Locale.ROOT)

    /** [instant] as e.g. `17:16:56 21.10.2026` in [zone]. */
    fun format(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        FORMAT.format(instant.atZone(zone))
}
