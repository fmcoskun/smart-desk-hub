package com.iot.widget.ui.logs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.DeviceRepository
import com.iot.widget.data.LogPage
import com.iot.widget.data.LogRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.logs.CsvExport
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogQuery
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** One line of the list. `who` = the user for CLIENT lines, the device for DEVICE lines. */
data class LogRowUi(
    val id: Long,
    val time: String,
    val source: LogSource,
    val level: LogLevel,
    val who: String,
    val message: String,
)

data class LogsUiState(
    val rows: List<LogRowUi> = emptyList(),
    val total: Int = 0,
    val query: LogQuery = LogQuery(),
    /** false for users without VIEW_LOGS (Guest): nothing is loaded. */
    val allowed: Boolean = false,
    val loaded: Boolean = false,
    /** What is typed in the search field (applied to the query after a short pause). */
    val text: String = "",
    val deviceName: String? = null,
)

/**
 * UI layer · the Logs tab (spec §6.9). The query is a StateFlow; `flatMapLatest` switches to a
 * new live database query whenever a filter changes (and cancels the old one), so the list is
 * always "the current filters, kept up to date".
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LogsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val logs: LogRepository,
    devices: DeviceRepository,
    private val session: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    /** `deviceId` comes from the "View logs" shortcut of a device page (LogsRoute argument). */
    private val query = MutableStateFlow(LogQuery(deviceId = savedStateHandle.get<String>("deviceId")))
    private val text = MutableStateFlow("")

    init {
        // debounce: search only after typing pauses for 300 ms, not on every key press
        text.debounce(SEARCH_DEBOUNCE_MS).onEach { t -> query.update { it.copy(text = t) } }.launchIn(viewModelScope)
    }

    private val allowed = session.currentUser.map { it?.can(Permission.VIEW_LOGS) == true }

    /**
     * The current local day, re-emitted at midnight. It is part of the query below, so with the
     * screen open across midnight "Today" moves on and "14:31" becomes "Tue 14:31".
     */
    private val today = flow {
        while (true) {
            val now = ZonedDateTime.now(clock)
            emit(now.toLocalDate())
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(clock.zone)
            delay(Duration.between(now, midnight).toMillis() + 1)
        }
    }.distinctUntilChanged()

    private val page = combine(query, allowed, today) { q, ok, day -> Triple(q, ok, day) }
        .flatMapLatest { (q, ok, day) ->
            (if (ok) logs.observe(q) else flowOf(LogPage(emptyList(), 0))).map { it to day }
        }

    val uiState: StateFlow<LogsUiState> = combine(page, query, text, allowed, devices.devices) { (p, day), q, t, ok, all ->
        LogsUiState(
            rows = p.entries.map { e ->
                LogRowUi(e.id, timeLabel(e.timeMillis, day), e.source, e.level, e.actor ?: e.deviceName.orEmpty(), e.message)
            },
            total = p.total,
            query = q,
            allowed = ok,
            loaded = true,
            text = t,
            deviceName = q.deviceId?.let { id -> all.firstOrNull { it.id == id }?.name ?: id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LogsUiState())

    fun setText(value: String) {
        text.value = value
    }

    /** Turning off the last remaining source would show nothing, so that tap is ignored. */
    fun toggleSource(source: LogSource) = query.update { q ->
        val next = if (source in q.sources) q.sources - source else q.sources + source
        if (next.isEmpty()) q else q.copy(sources = next)
    }

    fun toggleLevel(level: LogLevel) = query.update { q ->
        val next = if (level in q.levels) q.levels - level else q.levels + level
        if (next.isEmpty()) q else q.copy(levels = next)
    }

    fun setRange(range: DateRange) = query.update { it.copy(range = range) }

    fun clearDevice() = query.update { it.copy(deviceId = null) }

    /**
     * Every entry matching the current filters (not only the 500 on screen) as CSV, or null
     * for users without VIEW_LOGS. Uses the search text as typed, even inside the debounce.
     */
    suspend fun exportCsv(): String? {
        if (session.currentUser.first()?.can(Permission.VIEW_LOGS) != true) return null
        val entries = logs.export(query.value.copy(text = text.value))
        // up to 5 000 rows: build the text on a background thread, not on the UI thread
        return withContext(Dispatchers.Default) { CsvExport.format(entries, clock.zone) }
    }

    /** "14:31" for today, "Tue 14:31" for earlier days (English UI). */
    private fun timeLabel(millis: Long, today: LocalDate): String {
        val t = Instant.ofEpochMilli(millis).atZone(clock.zone)
        return if (t.toLocalDate() == today) HOUR_MINUTE.format(t) else DAY_HOUR_MINUTE.format(t)
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        private val HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
        private val DAY_HOUR_MINUTE = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.US)
    }
}
