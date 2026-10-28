package com.iot.widget.domain.logs

import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SampleLogsTest {

    private val now = ZonedDateTime.of(2026, 10, 28, 15, 0, 0, 0, ZoneId.of("Europe/Istanbul"))
    private val devices = mapOf(
        "soundbar" to "Bluetooth", "desk_light" to "Desk Halo LED", "smart_plug" to "Smart Plug",
        "privacy_guard" to "Privacy Guard", "cam_door" to "Door Camera", "room_sensor" to "Room Sensor",
        "meeting_sensor" to "Meeting Room Sensor",
    )
    private val users = listOf("Ayşe Yılmaz", "Mehmet Kaya")

    @Test
    fun about200SpreadOver7DaysSorted() {
        val logs = SampleLogs.generate(now, devices, users)
        assertTrue(logs.size in 180..220)
        val weekStart = DateRange.Last7Days.toMillis(now).first
        assertTrue(logs.all { it.timeMillis in weekStart..now.toInstant().toEpochMilli() })
        assertEquals(logs.sortedBy { it.timeMillis }, logs)
        assertEquals(LogSource.entries.toSet(), logs.map { it.source }.toSet())
        assertEquals(LogLevel.entries.toSet(), logs.map { it.level }.toSet())
        assertTrue(logs.any { it.payload != null && it.topic != null })
    }

    @Test
    fun publishersAreActiveUsersWhoMayControlDevices() {
        val all = listOf(
            User("ayse", "Ayşe Yılmaz", Role.ADMIN, true, null),
            User("guest", "Guest", Role.GUEST, true, null),
            User("old", "Old Operator", Role.OPERATOR, false, null),
            User("op", "Mehmet Kaya", Role.OPERATOR, true, null),
        )
        assertEquals(listOf("Ayşe Yılmaz", "Mehmet Kaya"), SampleLogs.publishers(all))
    }

    @Test
    fun noUsersFallsBackToSystem() {
        val logs = SampleLogs.generate(now, devices, emptyList())
        assertTrue(logs.filter { it.source == LogSource.CLIENT }.all { it.actor == "System" })
    }

    @Test
    fun deterministic() = assertEquals(SampleLogs.generate(now, devices, users), SampleLogs.generate(now, devices, users))
}
