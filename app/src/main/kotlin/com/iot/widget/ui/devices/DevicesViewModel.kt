package com.iot.widget.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.devices.DeviceRules
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.Room
import com.iot.widget.simulation.DeviceSimulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** List filters: everything, one room, or only devices that are offline. */
enum class DeviceFilter { ALL, DESK, MEETING_ROOM, HALLWAY, ENTRANCE, OFFLINE }

/** The second line of a row: battery level for battery devices, firmware otherwise. */
sealed interface DeviceSummary {
    data class Firmware(val version: String) : DeviceSummary
    data class Battery(val pct: Int) : DeviceSummary
    data object Restarting : DeviceSummary
}

data class DeviceRowUi(
    val id: String,
    val name: String,
    val type: DeviceType,
    val room: Room,
    val online: Boolean,
    val summary: DeviceSummary,
)

data class DevicesUiState(
    val rows: List<DeviceRowUi> = emptyList(),
    val filter: DeviceFilter = DeviceFilter.ALL,
    val counts: Map<DeviceFilter, Int> = emptyMap(),
    val canAdd: Boolean = false,
)

/** UI layer · the Devices tab (spec §6.8): every device, filterable by room or "offline". */
@HiltViewModel
class DevicesViewModel @Inject constructor(
    devices: DeviceRepository,
    session: SessionRepository,
    simulator: DeviceSimulator,
) : ViewModel() {

    private val filter = MutableStateFlow(DeviceFilter.ALL)

    val uiState: StateFlow<DevicesUiState> = combine(
        devices.devices,
        filter,
        session.currentUser,
        simulator.restarting,
    ) { all, selected, user, restarting ->
        val rows = all.map { d ->
            DeviceRowUi(
                id = d.id,
                name = d.name,
                type = d.type,
                room = d.room,
                online = d.online,
                summary = when {
                    d.id in restarting -> DeviceSummary.Restarting
                    d.info.batteryPct != null -> DeviceSummary.Battery(d.info.batteryPct)
                    else -> DeviceSummary.Firmware(d.info.firmware)
                },
            )
        }
        DevicesUiState(
            rows = rows.filter { it.matches(selected) },
            filter = selected,
            counts = DeviceFilter.entries.associateWith { f -> rows.count { it.matches(f) } },
            canAdd = user != null && DeviceRules.canAddRemove(user),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun setFilter(value: DeviceFilter) {
        filter.value = value
    }
}

private fun DeviceRowUi.matches(filter: DeviceFilter): Boolean = when (filter) {
    DeviceFilter.ALL -> true
    DeviceFilter.OFFLINE -> !online
    DeviceFilter.DESK -> room == Room.DESK
    DeviceFilter.MEETING_ROOM -> room == Room.MEETING_ROOM
    DeviceFilter.HALLWAY -> room == Room.HALLWAY
    DeviceFilter.ENTRANCE -> room == Room.ENTRANCE
}
