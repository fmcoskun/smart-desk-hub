package com.iot.widget.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RelativeTimeTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val now = ZonedDateTime.of(2026, 10, 27, 17, 0, 0, 0, zone)

    private fun at(t: ZonedDateTime?) = relativeTime(now.toInstant(), t?.toInstant(), zone)

    @Test
    fun never() = assertEquals(RelativeTime.Never, at(null))

    @Test
    fun justNow() = assertEquals(RelativeTime.Now, at(now.minusSeconds(30)))

    @Test
    fun minutes() = assertEquals(RelativeTime.MinutesAgo(59), at(now.minusMinutes(59)))

    @Test
    fun hoursSameDay() = assertEquals(RelativeTime.HoursAgo(2), at(now.minusHours(2)))

    @Test
    fun yesterdayAcrossMidnight() {
        val tenPast = ZonedDateTime.of(2026, 10, 28, 0, 10, 0, 0, zone)
        val before = ZonedDateTime.of(2026, 10, 27, 23, 50, 0, 0, zone)
        // 20 minutes ago, but on the previous calendar day → still "20 min ago" (minutes win under 1 h)
        assertEquals(RelativeTime.MinutesAgo(20), relativeTime(tenPast.toInstant(), before.toInstant(), zone))
        val earlier = ZonedDateTime.of(2026, 10, 27, 20, 0, 0, 0, zone)
        assertEquals(RelativeTime.Yesterday, relativeTime(tenPast.toInstant(), earlier.toInstant(), zone))
    }

    @Test
    fun days() = assertEquals(RelativeTime.DaysAgo(3), at(now.minusDays(3)))
}
