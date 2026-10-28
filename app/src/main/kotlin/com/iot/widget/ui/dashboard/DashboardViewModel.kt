package com.iot.widget.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.R
import com.iot.widget.automation.AutomationScheduler
import com.iot.widget.control.DeskCommands
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.domain.AudioOutput
import com.iot.widget.domain.DeskState
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Formatting
import com.iot.widget.domain.Payloads
import com.iot.widget.domain.Scene
import com.iot.widget.domain.Topics
import com.iot.widget.domain.devices.BuiltIn
import com.iot.widget.domain.powerDrawWatts
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import com.iot.widget.simulation.SensorSimulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.ceil
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * UI layer · the dashboard's ViewModel (the "VM" of MVVM).
 *
 * A ViewModel survives screen rotation and holds the screen's logic: it turns repository data
 * into a [DashboardUiState] and turns user taps (onToggle, onScene...) into repository writes
 * and MQTT messages. The Composable only draws the state and forwards taps — it never talks to
 * the database itself.
 *
 * @HiltViewModel + @Inject constructor: Hilt creates it with all dependencies filled in.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: DeskRepository,
    private val publisher: MqttPublisher,
    sensors: SensorSimulator,
    private val scheduler: AutomationScheduler,
    private val session: SessionRepository,
    devices: DeviceRepository,
    private val clock: Clock,
    private val random: Random,
) : ViewModel() {

    // null = not known yet (the session is still loading). Eagerly: onToggle & co. need the
    // answer synchronously, even before the screen collects. Unknown counts as "not allowed".
    private val canControl: StateFlow<Boolean?> = session.currentUser
        .map { it?.can(Permission.CONTROL_DEVICES) == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Switching rules shared with the home-screen widget (built here, so the constructor stays as it is). */
    private val commands = DeskCommands(repo, publisher, scheduler, session, clock)

    /** Slider position while the finger moves; null when the saved value is current. */
    private val pendingBrightness = MutableStateFlow<Int?>(null)
    private var dimJob: Job? = null

    // A new random "ping" only when a new message is published (not on every sensor tick)
    private val messageWithPing = publisher.lastMessage.map { it to random.nextInt(11, 19) }

    // Re-emits twice a minute so the "Off in N min" label counts down while visible
    private val ticker = flow {
        while (true) {
            emit(clock.millis())
            delay(30_000)
        }
    }

    // The footer's temperature/humidity come from the Room Sensor: offline = no reading
    private val roomSensorOnline = devices.devices.map { all -> all.firstOrNull { it.id == BuiltIn.ROOM_SENSOR }?.online ?: true }

    /**
     * stateIn turns the combined streams into one StateFlow the screen can read.
     * WhileSubscribed(5_000): the streams (including the sensor simulation) run only while the
     * screen is visible, and stop 5 s after it goes away — the Compose equivalent of the old
     * onStart/onStop handling. The 5 s grace period keeps them alive across a rotation.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        combine(repo.state, canControl, roomSensorOnline) { desk, allowed, sensorOn -> Triple(desk, allowed, sensorOn) },
        pendingBrightness,
        sensors.readings().onStart { emit(sensors.latest) },
        messageWithPing,
        ticker,
    ) { (desk, allowed, sensorOn), pending, reading, (message, ping), nowMillis ->
        val brightness = pending ?: desk.settings.brightness
        val shown = desk.copy(settings = desk.settings.copy(brightness = brightness))
        DashboardUiState(
            // not drawn until the permission is known, so an Admin never sees a "Guest" frame
            loaded = allowed != null,
            devices = desk.devices.values.map { cardFor(it.id, shown, nowMillis) },
            brightness = brightness,
            lightOn = desk.devices[DeviceId.DeskLight]?.isOn == true,
            activeScene = desk.settings.activeScene,
            lastMessage = message,
            pingMs = if (message == null) 14 else ping,
            temperature = if (sensorOn) Formatting.temperature(reading.temperatureC) else NO_READING,
            humidity = if (sensorOn) Formatting.humidity(reading.humidityPct) else NO_READING,
            power = Formatting.watts(powerDrawWatts(shown)),
            canControl = allowed == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun onToggle(id: DeviceId, on: Boolean) {
        // Guard here too, not only in the UI: a disabled control is not a security boundary
        if (canControl.value != true) return
        // viewModelScope: coroutines here are cancelled automatically when the ViewModel is destroyed
        // the rules (offline, plug timer, MQTT messages) are shared with the home-screen widget
        viewModelScope.launch { commands.toggle(id, on) }
    }

    /** Called for every slider movement; saving and publishing wait until 150 ms of stillness. */
    fun onBrightnessChange(value: Int) {
        if (canControl.value != true) return
        if (!lightReachable()) return // the dimmer of an unreachable light does nothing
        pendingBrightness.value = value
        debounceDim(value, persist = true)
    }

    fun onScene(scene: Scene) {
        if (canControl.value != true) return
        viewModelScope.launch {
            // the dim message goes through this screen's 150 ms debounce, so not from commands
            val result = commands.scene(scene, publishDim = false) ?: return@launch
            // the scene did not touch an unreachable light, so it gets no dim message either
            if (result.state.devices[DeviceId.DeskLight]?.online != false) {
                debounceDim(result.state.settings.brightness, persist = false)
            }
        }
    }

    private fun debounceDim(value: Int, persist: Boolean) {
        dimJob?.cancel() // a newer value replaces the pending one: this is the debounce
        dimJob = viewModelScope.launch {
            delay(DIM_DEBOUNCE_MS)
            if (persist) repo.updateSettings { it.copy(brightness = value) }
            pendingBrightness.value = null
            publish(Topics.LIGHTS_DIM, Payloads.brightness(value))
        }
    }

    private fun lightReachable(): Boolean =
        uiState.value.devices.firstOrNull { it.id == DeviceId.DeskLight }?.online != false

    private fun cardFor(id: DeviceId, desk: DeskState, nowMillis: Long): DeviceCardUi {
        val device = desk.devices.getValue(id)
        val settings = desk.settings
        return DeviceCardUi(
            id = id,
            detail = DETAIL_OF.getValue(id),
            name = device.name,
            subtitle = device.subtitle,
            statusRes = when {
                id != DeviceId.Soundbar -> null
                !device.isOn -> R.string.bt_disconnected
                settings.audioOutput == AudioOutput.Headphones -> R.string.bt_headphones_connected
                else -> R.string.bt_connected
            },
            isOn = device.isOn,
            online = device.online,
            timerMinutesLeft = settings.plugTimerEndsAtMillis
                ?.takeIf { id == DeviceId.SmartPlug && device.isOn && it > nowMillis }
                ?.let { ceil((it - nowMillis) / 60_000.0).toInt() },
            // the end time has passed but the auto-off job has not run yet (Android may delay it)
            timerOverdue = id == DeviceId.SmartPlug && device.isOn &&
                settings.plugTimerEndsAtMillis?.let { it <= nowMillis } == true,
        )
    }

    private fun publish(topic: String, json: String) = publisher.publish(MqttMessage(topic, json))


    companion object {
        const val DIM_DEBOUNCE_MS = 150L
        const val NO_READING = "—"
        val DETAIL_OF = mapOf(
            DeviceId.Soundbar to DetailType.Bluetooth,
            DeviceId.DeskLight to DetailType.Light,
            DeviceId.SmartPlug to DetailType.Rig,
            DeviceId.PrivacyGuard to DetailType.Privacy,
        )
    }
}
