package com.iot.widget.domain.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LogRulesTest {

    private val ist = ZoneId.of("Europe/Istanbul")
    private val now = ZonedDateTime.of(2026, 10, 28, 15, 0, 0, 0, ist)

    private fun entry(message: String, actor: String? = null, device: String? = null, topic: String? = null) =
        LogEntry(0, 0, LogSource.CLIENT, LogLevel.INFO, actor, null, device, message, topic, null)

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0, s: Int = 0) =
        ZonedDateTime.of(y, mo, d, h, mi, s, 0, ist).toInstant().toEpochMilli()

    @Test
    fun textMatchFoldsTurkishAndCase() {
        assertTrue(entry("IŞIK SENSÖRÜ timeout").matches("ışık"))
        assertTrue(entry("Işık açıldı").matches("ışık"))
        assertTrue(entry("signed in", actor = "Ayşe Yılmaz").matches("AYŞE"))
        assertTrue(entry("ack ON", device = "Smart Plug").matches("plug"))
        assertTrue(entry("published", topic = "office/desk/lights").matches("LIGHTS"))
        assertFalse(entry("telemetry ok").matches("battery"))
    }

    @Test
    fun blankTextMatchesAll() = assertTrue(entry("anything").matches("   "))

    @Test
    fun rangesUseLocalDays() {
        val today = DateRange.Today.toMillis(now)
        assertEquals(millis(2026, 10, 28, 0), today.first)
        assertEquals(now.toInstant().toEpochMilli(), today.last)

        val yesterday = DateRange.Yesterday.toMillis(now)
        assertTrue(millis(2026, 10, 27, 23, 59, 59) in yesterday)
        assertFalse(millis(2026, 10, 28, 0) in yesterday)
        assertTrue(millis(2026, 10, 27, 0) in yesterday)

        val week = DateRange.Last7Days.toMillis(now)
        assertEquals(millis(2026, 10, 22, 0), week.first)
        assertFalse(millis(2026, 10, 21, 23, 59, 59) in week)
    }

    @Test
    fun customRangeIncludesEndDayAndSwapsReversedDays() {
        val r = DateRange.Custom(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 24)).toMillis(now)
        assertEquals(millis(2026, 10, 24, 0), r.first)
        assertTrue(millis(2026, 10, 26, 23, 59, 59) in r)
        assertFalse(millis(2026, 10, 27, 0) in r)
    }
}
