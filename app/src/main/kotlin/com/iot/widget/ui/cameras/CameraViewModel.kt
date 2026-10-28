package com.iot.widget.ui.cameras

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.cameras.Clips
import com.iot.widget.domain.isOn
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * UI layer · state of one camera's full-screen page (see [CameraViewModel]).
 */
data class CameraUiState(
    /** null while loading, or if the camera was deleted. */
    val tile: CameraTileUi? = null,
    val clock: String = "",
    val canViewRecordings: Boolean = false,
    /** How many snapshots were taken here; each new one shows "Snapshot saved" again. */
    val snapshots: Int = 0,
)

/** UI layer · one camera full screen (spec §6.6): the same feed rules as the grid, plus Snapshot. */
@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val devices: DeviceRepository,
    private val desk: DeskRepository,
    private val session: SessionRepository,
    private val eventLog: EventLog,
    clock: Clock,
) : ViewModel() {

    private val id: String = checkNotNull(savedStateHandle.get<String>("id"))
    private val snapshots = MutableStateFlow(0)

    val uiState: StateFlow<CameraUiState> = combine(
        devices.devices,
        desk.state.map { it.isOn(DeviceId.PrivacyGuard) },
        session.currentUser,
        clockTicker(clock),
        snapshots,
    ) { all, privacy, user, time, taken ->
        CameraUiState(
            tile = all.firstOrNull { it.id == id && it.id in Clips.LIVE }?.let { cameraTile(it, privacy) },
            clock = time,
            canViewRecordings = user?.can(Permission.VIEW_RECORDINGS) == true,
            snapshots = taken,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CameraUiState())

    /**
     * Logs "took a snapshot" under this camera. Checked against the latest data, not the
     * screen state: a camera that just went offline or dark cannot be photographed.
     */
    fun snapshot() {
        viewModelScope.launch {
            val user = session.currentUser.first() ?: return@launch
            val device = devices.byId(id) ?: return@launch
            val tile = cameraTile(device, desk.current().isOn(DeviceId.PrivacyGuard))
            if (tile.feed != FeedState.LIVE) return@launch
            eventLog.record(user.name, "took a snapshot", device.id, device.name)
            snapshots.update { it + 1 }
        }
    }
}
