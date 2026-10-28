package com.iot.widget.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.automation.AutomationScheduler
import com.iot.widget.automation.ExactAlarms
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.domain.AudioOutput
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Payloads
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.ScheduleDays
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import com.iot.widget.ui.dashboard.DashboardViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject

/** What a detail screen draws: its device plus all desk settings (with an unsaved volume overlay). */
data class DetailUiState(
    val type: DetailType,
    val deviceName: String,
    val deviceOn: Boolean,
    val settings: DeskSettings,
    /** False for users without CONTROL_DEVICES (Guest): the screen shows controls disabled. */
    val canControl: Boolean = true,
    /** false while the device is unreachable (offline/restarting): its settings cannot change. */
    val deviceOnline: Boolean = true,
)

/**
 * UI layer · ViewModel shared by the four detail screens (Bluetooth, LED, Smart Plug, Privacy).
 *
 * SavedStateHandle holds the navigation arguments (`type`) and small values that must survive
 * the app being killed in the background. A rotation does not re-create a ViewModel at all
 * (init runs once); but if Android kills the backgrounded app and the user returns, a *new*
 * ViewModel is created for the restored screen — the flag stored here stops that new one
 * from sending the "detail opened" message a second time.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repo: DeskRepository,
    private val publisher: MqttPublisher,
    private val scheduler: AutomationScheduler,
    private val exactAlarms: ExactAlarms,
    session: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    val type: DetailType = DetailType.fromKey(checkNotNull(savedStateHandle.get<String>("type")))
    private val deviceId: DeviceId = DashboardViewModel.DETAIL_OF.entries.first { it.value == type }.key

    private val pendingVolume = MutableStateFlow<Int?>(null)
    private var volumeJob: Job? = null

    // null = not known yet. Eagerly: the setters below need the answer synchronously.
    // Unknown counts as "not allowed".
    private val canControl: StateFlow<Boolean?> = session.currentUser
        .map { it?.can(Permission.CONTROL_DEVICES) == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // null until both the desk and the permission are known (the page draws nothing meanwhile)
    // Settings of an unreachable device cannot be changed (same rule as the dashboard card)
    private val deviceOnline: StateFlow<Boolean> = repo.state
        .map { it.devices[deviceId]?.online != false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val uiState: StateFlow<DetailUiState?> = combine(repo.state, pendingVolume, canControl) { desk, volume, allowed ->
        if (allowed == null) return@combine null
        val device = desk.devices.getValue(deviceId)
        DetailUiState(
            type = type,
            deviceName = device.name,
            deviceOn = device.isOn,
            settings = desk.settings.copy(volume = volume ?: desk.settings.volume),
            canControl = allowed,
            deviceOnline = device.online,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (savedStateHandle.get<Boolean>(KEY_PUBLISHED) != true) {
            savedStateHandle[KEY_PUBLISHED] = true
            publish(Topics.WIDGET_DETAIL, Payloads.detailOpen(type))
        }
    }

    fun setAudioOutput(output: AudioOutput) = save(Topics.SOUNDBAR_OUTPUT, Payloads.value(output.key)) {
        it.copy(audioOutput = output)
    }

    fun setColorTemp(kelvin: Int) = save(Topics.LIGHTS_CT, Payloads.value(kelvin.toString())) {
        it.copy(colorTempK = kelvin)
    }

    fun setAutoDim(on: Boolean) {
        if (!canChange()) return
        viewModelScope.launch {
            repo.updateSettings { it.copy(autoDim = on) }
            scheduler.scheduleSunset(repo.current().settings) // plans or cancels the sunset job
            publish(Topics.LIGHTS_AUTODIM, Payloads.state(on))
        }
    }

    /** Smart Plug auto-off timer: Off cancels it; any duration (re)starts it and turns the plug on. */
    fun setPlugTimer(timer: PlugTimer) {
        if (!canChange()) return
        viewModelScope.launch {
            if (timer == PlugTimer.Off) {
                scheduler.cancelPlugTimer()
                repo.updateSettings { it.copy(plugTimer = PlugTimer.Off, plugTimerEndsAtMillis = null) }
            } else {
                if (!repo.current().isOn(DeviceId.SmartPlug)) {
                    repo.setDeviceOn(DeviceId.SmartPlug, true)
                    publish(Topics.PLUG, Payloads.toggle(true, ZonedDateTime.now(clock)))
                }
                val endsAt = scheduler.startPlugTimer(timer, clock.instant())
                repo.updateSettings { it.copy(plugTimer = timer, plugTimerEndsAtMillis = endsAt) }
            }
            publish(Topics.PLUG_TIMER, Payloads.value(timer.key))
        }
    }

    /** LED schedule editor "Save": stores the times, re-plans the jobs, publishes the schedule. */
    fun setSchedule(on: LocalTime, onDays: ScheduleDays, off: LocalTime, offDays: ScheduleDays, enabled: Boolean) {
        if (!canChange()) return
        viewModelScope.launch {
            repo.updateSettings {
                it.copy(scheduleOn = on, scheduleOnDays = onDays, scheduleOff = off, scheduleOffDays = offDays, scheduleEnabled = enabled)
            }
            scheduler.scheduleLight(repo.current().settings)
            publish(Topics.LIGHTS_SCHEDULE, Payloads.schedule(on, onDays, off, offDays, enabled))
        }
    }

    fun setPrivacyOnLeave(on: Boolean) = save(Topics.PRIVACY_ON_LEAVE, Payloads.state(on)) {
        it.copy(privacyOnLeave = on)
    }

    fun setPrivacyOnLock(on: Boolean) = save(Topics.PRIVACY_ON_LOCK, Payloads.state(on)) {
        it.copy(privacyOnLock = on)
    }

    /** Volume slider: shown at once, saved and published after 150 ms without movement. */
    fun setVolume(value: Int) {
        if (!canChange()) return
        pendingVolume.value = value
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch {
            delay(DashboardViewModel.DIM_DEBOUNCE_MS)
            repo.updateSettings { it.copy(volume = value) }
            pendingVolume.value = null
            publish(Topics.SOUNDBAR_VOLUME, Payloads.value(value))
        }
    }

    /** Allowed to change this device's settings: may control devices AND the device is reachable. */
    private fun canChange(): Boolean = canControl.value == true && deviceOnline.value

    /** Whether automations can use exact alarms ("Alarms & reminders" granted). */
    fun exactAlarmsAllowed(): Boolean = exactAlarms.canSchedule()

    private fun save(topic: String, json: String, transform: (DeskSettings) -> DeskSettings) {
        // Guard here too, not only in the UI: a disabled control is not a security boundary
        if (!canChange()) return
        viewModelScope.launch {
            repo.updateSettings(transform)
            publish(topic, json)
        }
    }

    private fun publish(topic: String, json: String) = publisher.publish(MqttMessage(topic, json))

    private companion object {
        const val KEY_PUBLISHED = "detail_published"
    }
}
