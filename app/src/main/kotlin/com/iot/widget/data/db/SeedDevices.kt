package com.iot.widget.data.db

/**
 * Data layer · the built-in demo devices (spec §6.8, plan step 3). Dashboard devices first,
 * then the four cameras, then the room sensors; user-added devices come after (higher order).
 * Meeting Room Sensor starts offline with a weak battery, to show those states.
 */
object SeedDevices {

    fun all(nowMillis: Long): List<DeviceEntity> = listOf(
        device("soundbar", "Bluetooth", "Audio Engine Connected", "SOUNDBAR", "DESK", 0, "2.4.1", 21, null, -42, nowMillis),
        device("desk_light", "Desk Halo LED", "Philips Hue ScreenBar", "LIGHT", "DESK", 1, "1.8.0", 22, null, -45, nowMillis),
        device("smart_plug", "Smart Plug", "Peripherals & Monitors", "PLUG", "DESK", 2, "3.1.2", 23, null, -48, nowMillis),
        device("privacy_guard", "Privacy Guard", "Mic & Webcam Shutter", "PRIVACY", "DESK", 3, "1.0.6", 24, null, -55, nowMillis, isOn = false),
        device("cam_desk", "Desk Camera", "", "CAMERA", "DESK", 10, "5.2.0", 31, null, -50, nowMillis),
        device("cam_door", "Door Camera", "", "CAMERA", "ENTRANCE", 11, "5.2.0", 32, null, -58, nowMillis),
        device("cam_meeting", "Meeting Room Camera", "", "CAMERA", "MEETING_ROOM", 12, "5.2.0", 33, null, -61, nowMillis),
        device("cam_hallway", "Hallway Camera", "", "CAMERA", "HALLWAY", 13, "5.2.0", 34, null, -55, nowMillis),
        device("room_sensor", "Room Sensor", "", "SENSOR", "DESK", 20, "1.2.3", 41, 78, -60, nowMillis),
        device(
            "meeting_sensor", "Meeting Room Sensor", "", "SENSOR", "MEETING_ROOM", 21, "1.2.3", 42, 12, -79,
            lastSeen = nowMillis - 3 * 60 * 60_000L, online = false,
        ),
    )

    private fun device(
        id: String,
        name: String,
        subtitle: String,
        type: String,
        room: String,
        order: Int,
        firmware: String,
        ipLastByte: Int,
        battery: Int?,
        signal: Int,
        lastSeen: Long,
        online: Boolean = true,
        isOn: Boolean = true,
    ) = DeviceEntity(
        id = id,
        name = name,
        subtitle = subtitle,
        type = type,
        room = room,
        isOn = isOn,
        online = online,
        sortOrder = order,
        firmware = firmware,
        ipAddress = "192.168.1.$ipLastByte",
        macAddress = "24:6F:28:A1:00:%02X".format(ipLastByte),
        batteryPct = battery,
        signalDbm = signal,
        lastSeenMillis = lastSeen,
    )
}
