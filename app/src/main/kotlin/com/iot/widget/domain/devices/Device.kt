package com.iot.widget.domain.devices

/**
 * Domain layer · everything the device manager knows about a device (spec §6.8). The dashboard
 * still works with its smaller DeskState; both read the same `devices` rows, so a rename here
 * shows up on the dashboard card too (spec §9a: one source of truth).
 */
enum class DeviceType { SOUNDBAR, LIGHT, PLUG, PRIVACY, CAMERA, SENSOR }

/** The fixed set of rooms of the demo office. */
enum class Room { DESK, MEETING_ROOM, HALLWAY, ENTRANCE }

data class DeviceInfo(
    val firmware: String,
    val ipAddress: String,
    val macAddress: String,
    /** null = mains powered (no battery). */
    val batteryPct: Int?,
    val signalDbm: Int,
    val lastSeenMillis: Long,
)

data class Device(
    val id: String,
    val name: String,
    val type: DeviceType,
    val room: Room,
    val online: Boolean,
    val isOn: Boolean,
    val info: DeviceInfo,
)

/** The devices that ship with the app. They can be renamed or moved, never deleted. */
object BuiltIn {
    val DASHBOARD_IDS = setOf("soundbar", "desk_light", "smart_plug", "privacy_guard")
    const val ROOM_SENSOR = "room_sensor"
    const val MEETING_SENSOR = "meeting_sensor"
    val CAMERA_IDS = listOf("cam_desk", "cam_door", "cam_meeting", "cam_hallway")
    val ALL: Set<String> = DASHBOARD_IDS + CAMERA_IDS + ROOM_SENSOR + MEETING_SENSOR
}

val Device.isBuiltIn: Boolean get() = id in BuiltIn.ALL
val Device.isOnDashboard: Boolean get() = id in BuiltIn.DASHBOARD_IDS
