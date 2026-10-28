package com.iot.widget.ui.recordings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.RecordingRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.cameras.RecordingEvent
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** What the player page shows about the recording; [clip] is the raw video to play. */
data class PlayerInfoUi(
    val clip: String,
    val camera: String,
    val started: String,
    val durationMs: Long,
    val event: RecordingEvent,
)

data class PlayerUiState(
    val info: PlayerInfoUi? = null,
    val allowed: Boolean = false,
    val loaded: Boolean = false,
    /** Allowed, but no recording has this id. */
    val notFound: Boolean = false,
)

/**
 * UI layer · loads one recording for the player. The ExoPlayer itself lives in the screen
 * (it is a view-level object tied to the screen's lifecycle); this keeps only the data.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recordings: RecordingRepository,
    devices: DeviceRepository,
    session: SessionRepository,
    clock: Clock,
) : ViewModel() {

    private val id: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    val uiState: StateFlow<PlayerUiState> = flow {
        // the same permission as the list: a stale back stack or shortcut cannot bypass it
        val user = session.currentUser.first()
        if (user?.can(Permission.VIEW_RECORDINGS) != true) {
            emit(PlayerUiState(allowed = false, loaded = true))
            return@flow
        }
        val rec = recordings.byId(id)
        val info = rec?.let {
            PlayerInfoUi(
                clip = it.clip,
                camera = devices.byId(it.cameraId)?.name ?: it.cameraId,
                started = STARTED.format(Instant.ofEpochMilli(it.startMillis).atZone(clock.zone)),
                durationMs = it.durationMs,
                event = it.event,
            )
        }
        emit(PlayerUiState(info = info, allowed = true, loaded = true, notFound = info == null))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlayerUiState())

    private companion object {
        val STARTED: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm:ss", Locale.US)
    }
}
