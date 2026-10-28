package com.iot.widget.domain

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Domain layer · "how long ago" as a value, not text: the UI turns it into "5 min ago",
 * "yesterday"... from strings.xml, so the wording can be translated later.
 */
sealed interface RelativeTime {
    data object Never : RelativeTime
    data object Now : RelativeTime
    data class MinutesAgo(val n: Int) : RelativeTime
    data class HoursAgo(val n: Int) : RelativeTime
    data object Yesterday : RelativeTime
    data class DaysAgo(val n: Int) : RelativeTime
}

/**
 * Under a minute → Now; under an hour → minutes; same calendar day → hours; the previous
 * calendar day → Yesterday; otherwise the number of calendar days.
 */
fun relativeTime(now: Instant, then: Instant?, zone: ZoneId): RelativeTime {
    if (then == null) return RelativeTime.Never
    val elapsed = Duration.between(then, now)
    if (elapsed.toMinutes() < 1) return RelativeTime.Now
    if (elapsed.toMinutes() < 60) return RelativeTime.MinutesAgo(elapsed.toMinutes().toInt())
    val days = ChronoUnit.DAYS.between(then.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate()).toInt()
    return when (days) {
        0 -> RelativeTime.HoursAgo(elapsed.toHours().toInt())
        1 -> RelativeTime.Yesterday
        else -> RelativeTime.DaysAgo(days)
    }
}
