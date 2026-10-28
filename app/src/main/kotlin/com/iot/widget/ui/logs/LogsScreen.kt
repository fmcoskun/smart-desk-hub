package com.iot.widget.ui.logs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.components.SectionLabel
import com.iot.widget.ui.components.TextFieldNeu
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes
import com.iot.widget.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Amber for warnings (not part of the reference palette, which has no warning colour). */
private val WarnColor = Color(0xFFB26A00)

/** Callbacks of the Logs screen, bundled so tests can pass [NONE]. */
data class LogsActions(
    val onOpen: (Long) -> Unit,
    val onText: (String) -> Unit,
    val onSource: (LogSource) -> Unit,
    val onLevel: (LogLevel) -> Unit,
    val onRange: (DateRange) -> Unit,
    val onClearDevice: () -> Unit,
    val onExport: () -> Unit,
) {
    companion object {
        val NONE = LogsActions({}, {}, {}, {}, {}, {}, {})
    }
}

/** UI layer · the Logs tab (spec §6.9). */
@Composable
fun LogsScreen(onOpen: (Long) -> Unit, viewModel: LogsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.logs_export)
    // Export: build the CSV, write it off the main thread, then open the system share sheet
    // one export at a time: a double tap must not build two files and open two share sheets
    var exporting by remember { mutableStateOf(false) }
    val onExport: () -> Unit = {
        if (!exporting) {
            exporting = true
            scope.launch {
                try {
                    val csv = viewModel.exportCsv() ?: return@launch
                    val share = CsvShare(context.applicationContext, Clock.systemDefaultZone())
                    val file = withContext(Dispatchers.IO) { share.write(csv) }
                    context.startActivity(Intent.createChooser(share.shareIntent(file), chooserTitle))
                } finally {
                    exporting = false
                }
            }
        }
    }
    LogsContent(
        state,
        LogsActions(
            onOpen = onOpen,
            onText = viewModel::setText,
            onSource = viewModel::toggleSource,
            onLevel = viewModel::toggleLevel,
            onRange = viewModel::setRange,
            onClearDevice = viewModel::clearDevice,
            onExport = onExport,
        ),
    )
}

/**
 * The filters stay on top; the entries scroll below them in a LazyColumn, which composes only
 * the rows on screen (up to 500 rows would otherwise all be built, and rebuilt on every new
 * entry). `key = id` lets Compose keep each row when new ones arrive at the top.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogsContent(state: LogsUiState, actions: LogsActions) {
    var pickRange by rememberSaveable { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxSize()
                .windowCard(DeskShapes.Window)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(stringResource(R.string.logs_title_upper))
            if (!state.allowed) {
                if (state.loaded) Text(stringResource(R.string.logs_not_allowed), fontSize = 13.sp, color = DeskColors.TextMuted)
                return@Column
            }
            TextFieldNeu(value = state.text, onValueChange = actions.onText, label = stringResource(R.string.logs_search), capitalization = KeyboardCapitalization.None)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.logs_client), LogSource.CLIENT in state.query.sources) { actions.onSource(LogSource.CLIENT) }
                Chip(stringResource(R.string.logs_device), LogSource.DEVICE in state.query.sources) { actions.onSource(LogSource.DEVICE) }
                Chip(stringResource(R.string.logs_info), LogLevel.INFO in state.query.levels) { actions.onLevel(LogLevel.INFO) }
                Chip(stringResource(R.string.logs_warn), LogLevel.WARN in state.query.levels, color = WarnColor) { actions.onLevel(LogLevel.WARN) }
                Chip(stringResource(R.string.logs_error), LogLevel.ERROR in state.query.levels, color = DeskColors.Red) { actions.onLevel(LogLevel.ERROR) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.logs_today), state.query.range == DateRange.Today) { actions.onRange(DateRange.Today) }
                Chip(stringResource(R.string.logs_yesterday), state.query.range == DateRange.Yesterday) { actions.onRange(DateRange.Yesterday) }
                Chip(stringResource(R.string.logs_7days), state.query.range == DateRange.Last7Days) { actions.onRange(DateRange.Last7Days) }
                val custom = state.query.range as? DateRange.Custom
                Chip(custom?.let(::rangeLabel) ?: stringResource(R.string.logs_custom), custom != null, icon = Bi.CalendarRange) { pickRange = true }
            }
            state.deviceName?.let { name ->
                Chip(stringResource(R.string.logs_device_filter, name), active = true, icon = Bi.XLg, onClick = actions.onClearDevice)
            }
            NeuChipButton(
                stringResource(R.string.logs_export),
                Bi.Share,
                active = false,
                onClick = actions.onExport,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.total > 0, // nothing to export
            )
            if (state.rows.isEmpty()) {
                Text(stringResource(R.string.logs_empty), fontSize = 13.sp, color = DeskColors.TextMuted)
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(DeskShapes.Drawer)
                        .background(DeskColors.InsetBg),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    items(state.rows, key = { it.id }) { row -> LogLine(row) { actions.onOpen(row.id) } }
                }
                if (state.total > state.rows.size) {
                    Text(
                        stringResource(R.string.logs_capped, COUNT.format(state.rows.size), COUNT.format(state.total)),
                        fontSize = 11.sp,
                        color = DeskColors.TextMuted,
                    )
                }
            }
        }
    }
    if (pickRange) {
        RangeDialog(
            initial = state.query.range as? DateRange.Custom,
            onDismiss = { pickRange = false },
            onPick = { pickRange = false; actions.onRange(it) },
        )
    }
}

/** "1,234" — the UI is English, so English digit grouping. */
private val COUNT: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US)

/** "20 Oct – 27 Oct", or "20 Oct" for a single day. */
private fun rangeLabel(r: DateRange.Custom): String {
    val first = minOf(r.from, r.to)
    val last = maxOf(r.from, r.to)
    return if (first == last) DAY_MONTH.format(first) else DAY_MONTH.format(first) + " – " + DAY_MONTH.format(last)
}

@Composable
private fun Chip(text: String, active: Boolean, icon: Bi? = null, color: Color? = null, onClick: () -> Unit) =
    NeuChipButton(text, icon, active = active, onClick = onClick, modifier = Modifier.padding(2.dp), contentColor = color, selected = active)

/** One log line in JetBrains Mono: source badge, time, who, message (coloured by level). */
@Composable
private fun LogLine(row: LogRowUi, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(
            stringResource(if (row.source == LogSource.CLIENT) R.string.logs_client_upper else R.string.logs_device_upper),
            if (row.source == LogSource.CLIENT) PillStyle.Neutral else PillStyle.On,
        )
        Column(Modifier.weight(1f)) {
            Text(
                row.time + " · " + row.who,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 10.sp,
                color = DeskColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.message,
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                fontWeight = if (row.level == LogLevel.INFO) FontWeight.Normal else FontWeight.Medium,
                color = when (row.level) {
                    LogLevel.INFO -> DeskColors.LogText
                    LogLevel.WARN -> WarnColor
                    LogLevel.ERROR -> DeskColors.Red
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Material 3 date-range picker; it reports UTC midnights, turned back into calendar days here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeDialog(initial: DateRange.Custom?, onDismiss: () -> Unit, onPick: (DateRange) -> Unit) {
    fun utcMillis(day: LocalDate) = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val today = LocalDate.now()
    val state = rememberDateRangePickerState(
        // reopening the picker shows the range that is already chosen
        initialSelectedStartDateMillis = initial?.let { utcMillis(minOf(it.from, it.to)) },
        initialSelectedEndDateMillis = initial?.let { utcMillis(maxOf(it.from, it.to)) },
        // logs are never in the future
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= utcMillis(today)
            override fun isSelectableYear(year: Int) = year <= today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val start = state.selectedStartDateMillis
                val end = state.selectedEndDateMillis ?: start
                if (start != null && end != null) {
                    fun day(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(DateRange.Custom(day(start), day(end)))
                } else {
                    onDismiss()
                }
            }) { Text(stringResource(R.string.action_save), color = DeskColors.GreenText, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = DeskColors.TextMuted) } },
    ) {
        DateRangePicker(state = state, modifier = Modifier.heightIn(max = 520.dp))
    }
}
