package com.iot.widget.domain

import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Domain layer · MQTT topics and JSON payloads. Topic names and payload shapes are exactly
 * those of the reference app, so a broker/dashboard built for the desktop widget keeps working.
 */
object Topics {
    const val SOUNDBAR = "office/desk/soundbar"
    const val LIGHTS = "office/desk/lights"
    const val LIGHTS_DIM = "office/desk/lights/dim"
    const val PLUG = "office/desk/plug_main"
    const val PRIVACY = "office/desk/privacy_guard"
    const val SCENES = "office/scenes/set"
    const val WIDGET_EVENT = "system/widget/event"
    const val WIDGET_DETAIL = "system/widget/detail"
    const val SOUNDBAR_OUTPUT = "office/desk/soundbar/output"
    const val SOUNDBAR_VOLUME = "office/desk/soundbar/volume"
    const val LIGHTS_CT = "office/desk/lights/ct"
    const val LIGHTS_AUTODIM = "office/desk/lights/autodim"
    const val LIGHTS_SCHEDULE = "office/desk/lights/schedule"
    const val PLUG_TIMER = "office/desk/plug_main/timer"
    const val PRIVACY_ON_LEAVE = "office/desk/privacy_guard/on_leave"
    const val PRIVACY_ON_LOCK = "office/desk/privacy_guard/on_lock"

    fun toggleTopic(id: DeviceId): String = when (id) {
        DeviceId.Soundbar -> SOUNDBAR
        DeviceId.DeskLight -> LIGHTS
        DeviceId.SmartPlug -> PLUG
        DeviceId.PrivacyGuard -> PRIVACY
    }
}

object Payloads {
    const val SOURCE = "android-desk-widget"
    const val PRIVACY_NOTICE = "Privacy Mode Activated: Mic & Cam Cutoff"

    // Same text JavaScript's toLocaleTimeString() produced on an English device: "5:21:34 PM".
    // Locale.US is fixed on purpose, so the payload does not change with the phone's language.
    private val TIME = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US)
    private val HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    // buildJsonObject keeps keys in insertion order and toString() prints compact JSON
    private fun json(block: JsonObjectBuilder.() -> Unit): String = buildJsonObject(block).toString()

    fun toggle(on: Boolean, now: ZonedDateTime): String = json {
        put("state", if (on) "ON" else "OFF")
        put("timestamp", TIME.format(now))
        put("source", SOURCE)
    }

    fun state(on: Boolean): String = json { put("state", if (on) "ON" else "OFF") }

    fun value(v: String): String = json { put("value", v) }

    fun value(v: Int): String = json { put("value", v) }

    fun brightness(v: Int): String = json { put("brightness", v) }

    fun preset(scene: Scene): String = json { put("preset", scene.key.uppercase(Locale.ROOT)) }

    fun notice(msg: String): String = json { put("notice", msg) }

    fun detailOpen(type: DetailType): String = json { put("open", type.key) }

    fun schedule(on: LocalTime, onDays: ScheduleDays, off: LocalTime, offDays: ScheduleDays, enabled: Boolean): String =
        json {
            put("on", HOUR_MINUTE.format(on))
            put("onDays", onDays.name)
            put("off", HOUR_MINUTE.format(off))
            put("offDays", offDays.name)
            put("enabled", enabled)
        }
}
