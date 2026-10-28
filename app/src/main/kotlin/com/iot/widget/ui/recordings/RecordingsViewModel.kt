package com.iot.widget.ui.recordings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.RecordingRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.cameras.Clips
import com.iot.widget.domain.cameras.Playback
import com.iot.widget.domain.cameras.Recording
import com.iot.widget.domain.cameras.RecordingEvent
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * UI layer · one line of the Recordings list (see [RecordingsViewModel]).
 */
data class RecordingRowUi(val id: Long, val camera: String, val time: String, val duration: String, val event: RecordingEvent)

data class RecordingsUiState(
    val rows: List<RecordingRowUi> = emptyList(),
    /** null = all cameras. */
    val cameraId: String? = null,
    val range: DateRange = DateRange.Today,
    /** Filter chips: camera id → name. */
    val cameras: List<Pair<String, String>> = emptyList(),
    /** false for users without VIEW_RECORDINGS (Guest). */
    val allowed: Boolean = false,
    val loaded: Boolean = false,
)

private data class Filter(val cameraId: String?, val range: DateRange)

/** UI layer · the Recordings browser (spec §6.7): one day, one camera or all, newest first. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    recordings: RecordingRepository,
    devices: DeviceRepository,
    session: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    /** `cameraId` comes from the "Recordings" button of a camera page (RecordingsRoute argument). */
    private val filter = MutableStateFlow(Filter(savedStateHandle.get<String>("cameraId"), DateRange.Today))
    private val allowed = session.currentUser.map { it?.can(Permission.VIEW_RECORDINGS) == true }

    private val found = combine(filter, allowed) { f, ok -> f to ok }
        .flatMapLatest { (f, ok) -> if (ok) recordings.observe(f.cameraId, f.range) else flowOf(emptyList()) }

    val uiState: StateFlow<RecordingsUiState> = combine(found, filter, allowed, devices.devices) { list, f, ok, all ->
        val names = all.filter { it.id in Clips.LIVE }.associate { it.id to it.name }
        RecordingsUiState(
            rows = list.map { it.toRow(names) },
            cameraId = f.cameraId,
            range = f.range,
            cameras = names.toList(),
            allowed = ok,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingsUiState())

    fun setCamera(cameraId: String?) = filter.update { it.copy(cameraId = cameraId) }

    fun setRange(range: DateRange) = filter.update { it.copy(range = range) }

    private fun Recording.toRow(names: Map<String, String>) = RecordingRowUi(
        id = id,
        camera = names[cameraId] ?: cameraId,
        time = HOUR_MINUTE.format(Instant.ofEpochMilli(startMillis).atZone(clock.zone)),
        duration = Playback.formatDuration(durationMs),
        event = event,
    )

    private companion object {
        val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    }
}
