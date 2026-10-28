package com.iot.widget.ui.dashboard

import androidx.annotation.StringRes
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Scene

/**
 * UI layer · everything the dashboard draws, as one immutable value. The screen is a pure
 * function of this object: same state → same pixels. This is the "UiState" half of MVVM.
 */
data class DashboardUiState(
    val loaded: Boolean = false,
    val devices: List<DeviceCardUi> = emptyList(),
    val brightness: Int = 80,
    val lightOn: Boolean = true,
    val activeScene: Scene = Scene.Work,
    val lastMessage: MqttMessage? = null,
    val pingMs: Int = 14,
    val temperature: String = "22.4°C",
    val humidity: String = "48%",
    val power: String = "0W",
    /** False for users without CONTROL_DEVICES (Guest): switches, scenes and dimmer are disabled. */
    val canControl: Boolean = false,
)

/** One device card. `statusRes` (a strings.xml text) replaces `subtitle` when set. */
data class DeviceCardUi(
    val id: DeviceId,
    val detail: DetailType,
    val name: String,
    val subtitle: String,
    @StringRes val statusRes: Int? = null,
    val isOn: Boolean,
    /** false while the device is unreachable (e.g. restarting): the card is disabled. */
    val online: Boolean = true,
    /** Minutes until the plug auto-off timer fires, or null when no timer runs. */
    val timerMinutesLeft: Int? = null,
    /** "Turning off…": the timer ended but the plug has not been switched off yet. */
    val timerOverdue: Boolean = false,
)
