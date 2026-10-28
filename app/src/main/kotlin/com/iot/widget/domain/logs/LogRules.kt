package com.iot.widget.domain.logs

import com.iot.widget.domain.Names
import java.time.LocalDate
import java.time.ZonedDateTime

/** Domain layer · how much log is kept and shown. */
object LogRetention {
    /** Entries older than this are deleted at app start. */
    const val MAX_DAYS = 30L

    /** At most this many newest entries are kept. */
    const val MAX_ENTRIES = 5_000

    /** The list shows at most this many newest matches (the export has them all). */
    const val LIST_LIMIT = 500
}

/**
 * The time window of a range, in epoch millis (inclusive), using *local calendar days*:
 * "Yesterday" is yesterday 00:00 up to (not including) today 00:00 in the phone's time zone,
 * not "the last 24 hours".
 */
fun DateRange.toMillis(now: ZonedDateTime): LongRange {
    val today = now.toLocalDate()
    fun startOf(day: LocalDate) = day.atStartOfDay(now.zone).toInstant().toEpochMilli()
    val nowMillis = now.toInstant().toEpochMilli()
    return when (this) {
        DateRange.Today -> startOf(today)..nowMillis
        DateRange.Yesterday -> startOf(today.minusDays(1)) until startOf(today)
        DateRange.Last7Days -> startOf(today.minusDays(6))..nowMillis
        is DateRange.Custom -> {
            // a range picked "backwards" is simply turned around
            val first = minOf(from, to)
            val last = maxOf(from, to)
            startOf(first) until startOf(last.plusDays(1)) // the end day is included in full
        }
    }
}

/**
 * Free-text search: case-insensitive and Turkish-aware ("ışık" finds "IŞIK"), over the
 * message, who did it, the device name and the MQTT topic.
 */
fun LogEntry.matches(text: String): Boolean {
    if (text.isBlank()) return true
    val needle = Names.comparable(text)
    return listOfNotNull(message, actor, deviceName, topic).any { Names.comparable(it).contains(needle) }
}
