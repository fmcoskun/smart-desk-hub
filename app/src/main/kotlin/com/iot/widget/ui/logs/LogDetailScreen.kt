package com.iot.widget.ui.logs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.iot.widget.R
import com.iot.widget.data.LogRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.can
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.JetBrainsMonoFamily
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * UI layer · one log entry in full (the LOG ENTRY page): fields, message and pretty-printed JSON.
 */
data class LogDetailUi(val entry: LogEntry, val time: String, val prettyPayload: String?)

/** Loading, not allowed (no VIEW_LOGS), gone (e.g. pruned after 30 days), or the entry. */
sealed interface LogDetailState {
    data object Loading : LogDetailState
    data object NotAllowed : LogDetailState
    data object NotFound : LogDetailState
    data class Loaded(val ui: LogDetailUi) : LogDetailState
}

/** UI layer · loads one log entry; the payload is pretty-printed when it is JSON. */
@HiltViewModel
class LogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    logs: LogRepository,
    session: SessionRepository,
    clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<LogDetailState>(LogDetailState.Loading)
    val state: StateFlow<LogDetailState> = _state.asStateFlow()

    init {
        val id = checkNotNull(savedStateHandle.get<Long>("id"))
        viewModelScope.launch {
            // same rule as the list: a stale back stack or a link cannot bypass VIEW_LOGS
            if (session.currentUser.first()?.can(Permission.VIEW_LOGS) != true) {
                _state.value = LogDetailState.NotAllowed
                return@launch
            }
            val e = logs.byId(id)
            _state.value = if (e == null) {
                LogDetailState.NotFound
            } else {
                LogDetailState.Loaded(
                    LogDetailUi(
                        entry = e,
                        time = TIME.format(Instant.ofEpochMilli(e.timeMillis).atZone(clock.zone)),
                        prettyPayload = e.payload?.let(::pretty),
                    ),
                )
            }
        }
    }

    companion object {
        private val TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm:ss", Locale.US)
        private val PRETTY = Json { prettyPrint = true }

        /** Indented JSON for reading; anything that is not JSON is shown as it is. */
        fun pretty(payload: String): String = try {
            PRETTY.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(payload))
        } catch (e: IllegalArgumentException) {
            payload
        }
    }
}

@Composable
fun LogDetailScreen(onBack: () -> Unit, viewModel: LogDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubPage(title = stringResource(R.string.log_entry_upper), icon = Bi.JournalText, onBack = onBack) {
        val d = when (val s = state) {
            LogDetailState.Loading -> return@SubPage
            LogDetailState.NotAllowed -> return@SubPage Text(stringResource(R.string.logs_not_allowed), fontSize = 13.sp, color = DeskColors.TextMuted)
            LogDetailState.NotFound -> return@SubPage Text(stringResource(R.string.log_not_found), fontSize = 13.sp, color = DeskColors.TextMuted)
            is LogDetailState.Loaded -> s.ui
        }
        val e = d.entry
        DetailSection(null) {
            InsetBox {
                Field(stringResource(R.string.log_time), d.time)
                Field(stringResource(R.string.log_source), e.source.name)
                Field(stringResource(R.string.log_level), e.level.name)
                e.actor?.let { Field(stringResource(R.string.log_actor), it) }
                e.deviceName?.let { Field(stringResource(R.string.log_device), it) }
                e.topic?.let { Field(stringResource(R.string.log_topic), it) }
            }
        }
        DetailSection(stringResource(R.string.log_message_upper)) {
            Text(e.message, fontSize = 14.sp)
        }
        d.prettyPayload?.let { json ->
            DetailSection(stringResource(R.string.log_payload_upper)) {
                InsetBox { Text(json, fontFamily = JetBrainsMonoFamily, fontSize = 12.sp, color = DeskColors.LogText) }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = DeskColors.TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeskColors.LogText)
    }
}
