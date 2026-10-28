package com.iot.widget.domain

import java.time.LocalTime

/**
 * Domain layer · the app's vocabulary. Plain Kotlin with no Android imports, so it can be
 * unit-tested on the JVM and reused by any screen or background worker.
 *
 * `data class` = a class whose job is to hold values; Kotlin generates equals/copy/toString.
 * Everything here is immutable: to change something you make a copy (`state.copy(...)`),
 * which makes state changes easy to follow and safe across threads.
 */

/** The four devices of the dashboard. `key` is the stable id stored in the database. */
enum class DeviceId(val key: String) {
    Soundbar("soundbar"),
    DeskLight("desk_light"),
    SmartPlug("smart_plug"),
    PrivacyGuard("privacy_guard"),
}

/** Which detail screen a card opens; keys are the reference's `data-detail` values. */
enum class DetailType(val key: String) {
    Bluetooth("bluetooth"),
    Light("light"),
    Rig("rig"),
    Privacy("privacy"),
    ;

    companion object {
        fun fromKey(key: String): DetailType = entries.first { it.key == key }
    }
}

enum class Scene(val key: String) { Work("work"), Meet("meet"), Leave("leave") }

enum class AudioOutput(val key: String) { Soundbar("soundbar"), Headphones("headphones") }

enum class ScheduleDays { WEEKDAYS, EVERY_DAY }

/** Smart Plug auto-off timer; `key` is the value published to MQTT (reference `data-value`). */
enum class PlugTimer(val key: String, val minutes: Int?) {
    Off("off", null),
    M30("30", 30),
    H1("60", 60),
    H2("120", 120),
}

data class DeviceState(
    val id: DeviceId,
    val name: String,
    val subtitle: String,
    val isOn: Boolean,
    val online: Boolean = true,
)

/** Every setting of the dashboard and the four detail screens, with the reference defaults. */
data class DeskSettings(
    val brightness: Int = 80,
    val colorTempK: Int = 4000,
    val autoDim: Boolean = true,
    val scheduleOn: LocalTime = LocalTime.of(8, 30),
    val scheduleOnDays: ScheduleDays = ScheduleDays.WEEKDAYS,
    val scheduleOff: LocalTime = LocalTime.of(19, 0),
    val scheduleOffDays: ScheduleDays = ScheduleDays.EVERY_DAY,
    val scheduleEnabled: Boolean = true,
    val audioOutput: AudioOutput = AudioOutput.Soundbar,
    val volume: Int = 45,
    val plugTimer: PlugTimer = PlugTimer.Off,
    val plugTimerEndsAtMillis: Long? = null,
    val privacyOnLeave: Boolean = true,
    val privacyOnLock: Boolean = false,
    val activeScene: Scene = Scene.Work,
)

/** The whole desk at one moment: what the dashboard draws and what the database stores. */
data class DeskState(
    val devices: Map<DeviceId, DeviceState>,
    val settings: DeskSettings,
)

fun DeskState.isOn(id: DeviceId): Boolean = devices[id]?.isOn == true

/** An offline or restarting device cannot be reached: no command is sent to it (spec §9a). */
fun DeskState.isReachable(id: DeviceId): Boolean = devices[id]?.online != false

/** Returns a copy with one device switched on/off (and the plug timer cleared if the plug goes off). */
fun DeskState.withDevice(id: DeviceId, on: Boolean): DeskState {
    val device = devices[id] ?: return this
    val settings = if (id == DeviceId.SmartPlug && !on) {
        settings.copy(plugTimer = PlugTimer.Off, plugTimerEndsAtMillis = null)
    } else {
        settings
    }
    return copy(devices = devices + (id to device.copy(isOn = on)), settings = settings)
}

/** Slider limits of the dimmer (`<input type=range min=5 max=100>`). */
const val MIN_BRIGHTNESS = 5
const val MAX_BRIGHTNESS = 100
