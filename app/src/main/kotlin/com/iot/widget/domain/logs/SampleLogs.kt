package com.iot.widget.domain.logs

import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.User
import com.iot.widget.domain.users.can
import com.iot.widget.domain.Payloads
import com.iot.widget.domain.Scene
import com.iot.widget.domain.Topics
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * Domain layer · a believable week of history for the demo (spec §6.9: ~200 entries over 7
 * days), so the Logs screen is not empty on first launch. A fixed random seed makes it the
 * same on every phone and in tests; times are relative to "now", so it is always "last week".
 */
object SampleLogs {

    fun generate(now: ZonedDateTime, deviceNames: Map<String, String>, users: List<String>): List<LogEntry> {
        val actors = users.ifEmpty { listOf("System") } // no eligible user: never index an empty list
        val random = Random(2026)
        val nowMillis = now.toInstant().toEpochMilli()
        val entries = mutableListOf<LogEntry>()
        for (daysAgo in 6 downTo 0) {
            val dayStart = now.toLocalDate().minusDays(daysAgo.toLong()).atTime(7, 0).atZone(now.zone)
            repeat(random.nextInt(25, 36)) {
                val time = dayStart.plusMinutes(random.nextLong(14 * 60L))
                val millis = time.toInstant().toEpochMilli()
                if (millis <= nowMillis) entries += pick(random, millis, time, deviceNames, actors)
            }
        }
        return entries.sortedBy { it.timeMillis }
    }

    /** Sample actors: only active users whose role may control devices (never Guest). */
    fun publishers(users: List<User>): List<String> =
        users.filter { it.active && it.can(Permission.CONTROL_DEVICES) }.map { it.name }

    private fun pick(r: Random, t: Long, time: ZonedDateTime, names: Map<String, String>, users: List<String>): LogEntry {
        fun name(id: String) = names[id] ?: id
        val user = users[r.nextInt(users.size)]
        return when (r.nextInt(10)) {
            0 -> client(t, user, "signed in")
            1, 2 -> {
                val (topic, id) = listOf(Topics.LIGHTS to "desk_light", Topics.PLUG to "smart_plug", Topics.SOUNDBAR to "soundbar")[r.nextInt(3)]
                val on = r.nextBoolean()
                LogEntry(0, t, LogSource.CLIENT, LogLevel.INFO, user, id, name(id), "published $topic", topic, Payloads.toggle(on, time))
            }
            3 -> {
                val scene = Scene.entries[r.nextInt(Scene.entries.size)]
                LogEntry(0, t, LogSource.CLIENT, LogLevel.INFO, user, null, null, "published ${Topics.SCENES}", Topics.SCENES, Payloads.preset(scene))
            }
            4 -> device(t, "desk_light", name("desk_light"), LogLevel.INFO, if (r.nextBoolean()) "ack ON" else "ack OFF")
            5, 6 -> {
                val id = listOf("cam_door", "room_sensor")[r.nextInt(2)]
                device(t, id, name(id), LogLevel.INFO, "telemetry ok")
            }
            7 -> device(t, "meeting_sensor", name("meeting_sensor"), LogLevel.WARN, "low battery (12%)")
            8 -> device(t, "meeting_sensor", name("meeting_sensor"), LogLevel.ERROR, "connection timeout")
            else -> device(t, "smart_plug", name("smart_plug"), LogLevel.WARN, "restarting…")
        }
    }

    private fun client(t: Long, actor: String, message: String) =
        LogEntry(0, t, LogSource.CLIENT, LogLevel.INFO, actor, null, null, message, null, null)

    private fun device(t: Long, id: String, name: String, level: LogLevel, message: String) =
        LogEntry(0, t, LogSource.DEVICE, level, null, id, name, message, null, null)
}
