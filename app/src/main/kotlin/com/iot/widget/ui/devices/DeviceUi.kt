package com.iot.widget.ui.devices

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.iot.widget.R
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.Room
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.icons.Bi

/** UI layer · shared look of devices: icon per type, room names, status badge. */
val DeviceType.icon: Bi
    get() = when (this) {
        DeviceType.SOUNDBAR -> Bi.Bluetooth
        DeviceType.LIGHT -> Bi.LightbulbFill
        DeviceType.PLUG -> Bi.Outlet
        DeviceType.PRIVACY -> Bi.ShieldLockFill
        DeviceType.CAMERA -> Bi.CameraVideo
        DeviceType.SENSOR -> Bi.Thermometer
    }

@Composable
fun roomLabel(room: Room): String = stringResource(
    when (room) {
        Room.DESK -> R.string.room_desk
        Room.MEETING_ROOM -> R.string.room_meeting
        Room.HALLWAY -> R.string.room_hallway
        Room.ENTRANCE -> R.string.room_entrance
    },
)

@Composable
fun typeLabel(type: DeviceType): String = stringResource(
    when (type) {
        DeviceType.SOUNDBAR -> R.string.type_soundbar
        DeviceType.LIGHT -> R.string.type_light
        DeviceType.PLUG -> R.string.type_plug
        DeviceType.PRIVACY -> R.string.type_privacy
        DeviceType.CAMERA -> R.string.type_camera
        DeviceType.SENSOR -> R.string.type_sensor
    },
)

/** ONLINE (green), OFFLINE (red) or RESTARTING (grey). */
@Composable
fun DeviceStatusPill(online: Boolean, restarting: Boolean) = when {
    restarting -> Pill(stringResource(R.string.status_restarting), PillStyle.Neutral)
    online -> Pill(stringResource(R.string.status_online), PillStyle.On)
    else -> Pill(stringResource(R.string.status_offline), PillStyle.Off)
}
