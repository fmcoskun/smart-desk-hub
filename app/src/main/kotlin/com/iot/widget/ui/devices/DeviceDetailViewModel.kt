package com.iot.widget.ui.devices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.domain.Names
import com.iot.widget.domain.RelativeTime
import com.iot.widget.domain.devices.Device
import com.iot.widget.domain.devices.DeviceRules
import com.iot.widget.domain.devices.Room
import com.iot.widget.domain.relativeTime
import com.iot.widget.domain.users.RuleResult
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.User
import com.iot.widget.domain.users.can
import com.iot.widget.simulation.DeviceSimulator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * UI layer · state of one device page (see [DeviceDetailViewModel]).
 */
data class DeviceDetailUiState(
    val device: Device? = null,
    val lastSeen: RelativeTime = RelativeTime.Never,
    val restarting: Boolean = false,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    /** Users with VIEW_LOGS get a "View logs" shortcut (pre-filtered Logs tab). */
    val canViewLogs: Boolean = false,
    /** Non-null while the name is being edited. */
    val nameDraft: String? = null,
    val error: RuleViolation? = null,
    val deleted: Boolean = false,
    val loaded: Boolean = false,
)

/** What the user typed or chose on this page (kept apart from the live device data). */
private data class Local(val nameDraft: String? = null, val error: RuleViolation? = null, val deleted: Boolean = false)

/**
 * UI layer · one device (spec §6.8). Live data (the device row, the restarting flag, who is
 * signed in) is combined with the page's own local state (name draft, error). Every action
 * re-checks [DeviceRules] here, so hidden buttons are not the only protection.
 */
@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val devices: DeviceRepository,
    private val session: SessionRepository,
    private val simulator: DeviceSimulator,
    private val eventLog: EventLog,
    private val clock: Clock,
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle.get<String>("id"))
    private val local = MutableStateFlow(Local())
    private var deleting: Job? = null
    private var renaming: Job? = null

    val uiState: StateFlow<DeviceDetailUiState> = combine(
        devices.devices,
        session.currentUser,
        simulator.restarting,
        local,
    ) { all, user, restarting, l ->
        val device = all.firstOrNull { it.id == id }
        DeviceDetailUiState(
            device = device,
            // an online device is being heard from right now; the stored time is the last contact
            lastSeen = if (device?.online == true) RelativeTime.Now
            else relativeTime(clock.instant(), device?.info?.lastSeenMillis?.let(Instant::ofEpochMilli), clock.zone),
            restarting = id in restarting,
            canEdit = user != null && DeviceRules.canEdit(user),
            canDelete = user != null && device != null && DeviceRules.canDelete(user, device),
            canViewLogs = user?.can(Permission.VIEW_LOGS) == true,
            nameDraft = l.nameDraft,
            error = l.error,
            deleted = l.deleted,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceDetailUiState())

    fun startRename() = local.update { it.copy(nameDraft = uiState.value.device?.name.orEmpty(), error = null) }
    fun setNameDraft(name: String) = local.update { it.copy(nameDraft = name, error = null) }
    fun cancelRename() = local.update { it.copy(nameDraft = null, error = null) }

    fun saveName() {
        if (renaming?.isActive == true) return // one save at a time
        renaming = withEditor { user, device -> rename(user, device) }
    }

    private suspend fun rename(user: User, device: Device) {
        val draft = local.value.nameDraft ?: return
        if (Names.clean(draft) == device.name) { // nothing changed: just close the editor
            local.update { it.copy(nameDraft = null, error = null) }
            return
        }
        val others = devices.devices.first().filter { it.id != id }.map { it.name }
        when (val result = DeviceRules.validateName(draft, others)) {
            is RuleResult.Rejected -> local.update { it.copy(error = result.reason) }
            RuleResult.Ok -> {
                devices.rename(id, Names.clean(draft))
                eventLog.record(user.name, "renamed device ${device.name} to ${Names.clean(draft)}")
                local.update { it.copy(nameDraft = null, error = null) }
            }
        }
    }

    fun setRoom(room: Room) = withEditor { user, device ->
        if (device.room == room) return@withEditor
        devices.setRoom(id, room)
        eventLog.record(user.name, "moved device ${device.name} to ${room.name}")
    }

    fun restart() = withEditor { user, device ->
        // DeviceSimulator ignores a second restart while one is running
        if (simulator.restart(id)) eventLog.record(user.name, "restarted device ${device.name}")
    }

    fun delete() {
        if (deleting?.isActive == true || local.value.deleted) return // one delete at a time
        deleting = viewModelScope.launch {
            val user = session.currentUser.first() ?: return@launch
            val device = devices.byId(id) ?: return@launch
            if (!DeviceRules.canDelete(user, device)) return@launch
            devices.delete(id)
            eventLog.record(user.name, "deleted device ${device.name}")
            local.update { it.copy(deleted = true) }
        }
    }

    /** Runs [action] only for a signed-in user who may edit devices, on an existing device. */
    private fun withEditor(action: suspend (User, Device) -> Unit): Job =
        viewModelScope.launch {
            val user = session.currentUser.first() ?: return@launch
            if (!DeviceRules.canEdit(user)) return@launch
            val device = devices.byId(id) ?: return@launch
            action(user, device)
        }
}
