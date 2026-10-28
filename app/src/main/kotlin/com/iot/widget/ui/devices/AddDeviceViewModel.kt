package com.iot.widget.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.domain.Names
import com.iot.widget.domain.devices.Device
import com.iot.widget.domain.devices.DeviceInfo
import com.iot.widget.domain.devices.DeviceRules
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.PairingEvent
import com.iot.widget.domain.devices.PairingStep
import com.iot.widget.domain.devices.Room
import com.iot.widget.domain.devices.nextPairing
import com.iot.widget.domain.users.RuleResult
import com.iot.widget.domain.users.RuleViolation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/**
 * UI layer · state of the Add device flow (choose type → searching → found → name), see [AddDeviceViewModel].
 */
data class AddDeviceUiState(val step: PairingStep = PairingStep.ChooseType, val error: RuleViolation? = null)

/**
 * UI layer · the simulated "Add device" flow (spec §6.8, Admin only). The steps come from the
 * pure [nextPairing] state machine; this ViewModel only adds the timing (a 2 s "search"), the
 * permission check and the final save.
 */
@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val devices: DeviceRepository,
    private val session: SessionRepository,
    private val eventLog: EventLog,
    private val clock: Clock,
    private val random: Random,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDeviceUiState())
    val uiState: StateFlow<AddDeviceUiState> = _uiState.asStateFlow()

    /** Chosen once, so a repeated "Add" can never create a second device. */
    private val newId = "dev_" + UUID.randomUUID().toString().take(8)
    private var adding: Job? = null
    private var searching: Job? = null

    private fun send(event: PairingEvent, taken: Collection<String> = emptyList()) =
        _uiState.update { it.copy(step = nextPairing(it.step, event, taken), error = null) }

    fun chooseType(type: DeviceType) {
        // a new pick replaces the previous search: its timer must not end this one early
        searching?.cancel()
        searching = viewModelScope.launch {
            val user = session.currentUser.first() ?: return@launch
            if (!DeviceRules.canAddRemove(user)) return@launch
            send(PairingEvent.TypeChosen(type))
            delay(SEARCH_MS) // pretend to scan the network
            // ignored by the state machine if the user cancelled; suggests a name nobody uses
            send(PairingEvent.SearchFinished, devices.devices.first().map { it.name })
        }
    }

    fun continueToNaming() = send(PairingEvent.Continue)

    fun setName(name: String) {
        val naming = _uiState.value.step as? PairingStep.Naming ?: return
        send(PairingEvent.Named(name, naming.room))
    }

    fun setRoom(room: Room) {
        val naming = _uiState.value.step as? PairingStep.Naming ?: return
        send(PairingEvent.Named(naming.name, room))
    }

    fun cancel() {
        searching?.cancel()
        send(PairingEvent.Cancel)
    }

    fun add() {
        if (adding?.isActive == true) return
        val naming = _uiState.value.step as? PairingStep.Naming ?: return
        adding = viewModelScope.launch {
            val user = session.currentUser.first() ?: return@launch
            if (!DeviceRules.canAddRemove(user)) return@launch
            val others = devices.devices.first().map { it.name }
            when (val result = DeviceRules.validateName(naming.name, others)) {
                is RuleResult.Rejected -> _uiState.update { it.copy(error = result.reason) }
                RuleResult.Ok -> {
                    devices.add(newDevice(naming))
                    eventLog.record(user.name, "added device ${Names.clean(naming.name)}")
                    send(PairingEvent.Added)
                }
            }
        }
    }

    /** Plausible made-up network details for the simulated device. */
    private fun newDevice(n: PairingStep.Naming): Device {
        val host = 100 + random.nextInt(100)
        return Device(
            id = newId,
            name = Names.clean(n.name), // trimmed + NFC, as a rename stores it
            type = n.type,
            room = n.room,
            online = true,
            isOn = true,
            info = DeviceInfo(
                firmware = "1.0.${random.nextInt(10)}",
                ipAddress = "192.168.1.$host",
                macAddress = "24:6F:28:B2:00:%02X".format(host),
                batteryPct = if (n.type == DeviceType.SENSOR) 100 else null,
                signalDbm = -40 - random.nextInt(30),
                lastSeenMillis = clock.millis(),
            ),
        )
    }

    companion object {
        const val SEARCH_MS = 2_000L
    }
}
