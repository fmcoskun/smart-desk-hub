package com.iot.widget.ui.cameras

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.cameras.Clips
import com.iot.widget.domain.devices.Device
import com.iot.widget.domain.isOn
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** What a camera tile shows: the moving feed, the Privacy Guard blackout, or "Offline". */
enum class FeedState { LIVE, PRIVACY, OFFLINE }

data class CameraTileUi(val id: String, val name: String, val feed: FeedState, val clip: String)

data class CamerasUiState(
    val tiles: List<CameraTileUi> = emptyList(),
    /** "HH:mm:ss", the live overlay clock. */
    val clock: String = "",
    val canViewRecordings: Boolean = false,
)

/** The Desk camera is the webcam that Privacy Guard shutters (spec §9a). */
const val PRIVACY_CAMERA = "cam_desk"

/** Turns a camera device + the Privacy Guard switch into a tile. Shared by the grid and the full-screen page. */
fun cameraTile(device: Device, privacyOn: Boolean): CameraTileUi = CameraTileUi(
    id = device.id,
    name = device.name,
    feed = when {
        !device.online -> FeedState.OFFLINE // includes the ~5 s of a restart
        device.id == PRIVACY_CAMERA && privacyOn -> FeedState.PRIVACY
        else -> FeedState.LIVE
    },
    clip = Clips.LIVE.getValue(device.id),
)

/** A "HH:mm:ss" label that changes on every full second. */
fun clockTicker(clock: Clock): Flow<String> = flow {
    val format = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
    while (true) {
        val now = clock.millis()
        emit(format.format(Instant.ofEpochMilli(now).atZone(clock.zone)))
        delay(1_000 - now % 1_000) // wake up exactly at the next second
    }
}

/**
 * UI layer · the Cameras tab (spec §6.6). Live data from three places: the device list
 * (names, online/offline), the desk state (Privacy Guard) and who is signed in.
 */
@HiltViewModel
class CamerasViewModel @Inject constructor(
    devices: DeviceRepository,
    desk: DeskRepository,
    session: SessionRepository,
    clock: Clock,
) : ViewModel() {

    private val privacyOn = desk.state.map { it.isOn(DeviceId.PrivacyGuard) }.distinctUntilChanged()

    val uiState: StateFlow<CamerasUiState> = combine(
        devices.devices,
        privacyOn,
        session.currentUser,
        clockTicker(clock),
    ) { all, privacy, user, time ->
        CamerasUiState(
            // the four demo cameras that have a feed, in the device list's order
            tiles = all.filter { it.id in Clips.LIVE }.map { cameraTile(it, privacy) },
            clock = time,
            canViewRecordings = user?.can(Permission.VIEW_RECORDINGS) == true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CamerasUiState())
}
