package com.iot.widget.ui.recordings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.cameras.RecordingEvent
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.JetBrainsMonoFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Callbacks of the Recordings screen, bundled so tests can pass [NONE]. */
data class RecordingsActions(
    val onBack: () -> Unit,
    val onOpen: (Long) -> Unit,
    val onCamera: (String?) -> Unit,
    val onRange: (DateRange) -> Unit,
) {
    companion object {
        val NONE = RecordingsActions({}, {}, {}, {})
    }
}

/** UI layer · recordings of one day (spec §6.7). */
@Composable
fun RecordingsScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, viewModel: RecordingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecordingsContent(state, RecordingsActions(onBack, onOpen, viewModel::setCamera, viewModel::setRange))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordingsContent(state: RecordingsUiState, actions: RecordingsActions) {
    var pickDay by rememberSaveable { mutableStateOf(false) }
    SubPage(title = stringResource(R.string.recordings_title_upper), icon = Bi.Film, onBack = actions.onBack) {
        if (!state.allowed) {
            if (state.loaded) Text(stringResource(R.string.recordings_not_allowed), fontSize = 13.sp, color = DeskColors.TextMuted)
            return@SubPage
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.logs_today), state.range == DateRange.Today) { actions.onRange(DateRange.Today) }
            Chip(stringResource(R.string.logs_yesterday), state.range == DateRange.Yesterday) { actions.onRange(DateRange.Yesterday) }
            val custom = state.range as? DateRange.Custom
            Chip(custom?.let { DAY.format(it.from) } ?: stringResource(R.string.recordings_pick_day), custom != null, Bi.CalendarEvent) { pickDay = true }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.recordings_all), state.cameraId == null) { actions.onCamera(null) }
            state.cameras.forEach { (id, name) -> Chip(name, state.cameraId == id) { actions.onCamera(id) } }
        }
        if (state.rows.isEmpty()) {
            Text(stringResource(R.string.recordings_empty), fontSize = 13.sp, color = DeskColors.TextMuted)
        } else {
            InsetBox { state.rows.forEach { row -> RecordingLine(row) { actions.onOpen(row.id) } } }
        }
    }
    if (pickDay) {
        DayDialog(
            initial = (state.range as? DateRange.Custom)?.from,
            onDismiss = { pickDay = false },
            onPick = { pickDay = false; actions.onRange(it) },
        )
    }
}

@Composable
private fun Chip(text: String, active: Boolean, icon: Bi? = null, onClick: () -> Unit) =
    NeuChipButton(text, icon, active = active, onClick = onClick, modifier = Modifier.padding(2.dp), selected = active)

/** Event badge, camera, then "start · length" in JetBrains Mono. */
@Composable
private fun RecordingLine(row: RecordingRowUi, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Pill(row.event.name, if (row.event == RecordingEvent.PERSON) PillStyle.On else PillStyle.Neutral)
        Column(Modifier.weight(1f)) {
            Text(row.camera, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(row.time + " · " + row.duration, fontSize = 11.sp, fontFamily = JetBrainsMonoFamily, color = DeskColors.TextMuted)
        }
    }
}

/** Material 3 date picker for one day; like the Logs range picker it reports UTC midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDialog(initial: LocalDate?, onDismiss: () -> Unit, onPick: (DateRange) -> Unit) {
    fun utcMillis(day: LocalDate) = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val today = LocalDate.now()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let(::utcMillis), // reopening shows the chosen day
        selectableDates = object : SelectableDates { // recordings are never in the future
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= utcMillis(today)
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val ms = state.selectedDateMillis
                if (ms == null) {
                    onDismiss()
                } else {
                    val day = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(DateRange.Custom(day, day))
                }
            }) { Text(stringResource(R.string.action_save), color = DeskColors.GreenText, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = DeskColors.TextMuted) } },
    ) {
        DatePicker(state = state)
    }
}

private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.US)
