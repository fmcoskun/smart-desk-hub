package com.iot.widget.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.ui.components.IconBox
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.SectionLabel
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/** UI layer · the Devices tab (spec §6.8). */
@Composable
fun DevicesScreen(onOpen: (String) -> Unit, onAdd: () -> Unit, viewModel: DevicesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DevicesContent(state, onFilter = viewModel::setFilter, onOpen = onOpen, onAdd = onAdd)
}

@Composable
fun DevicesContent(state: DevicesUiState, onFilter: (DeviceFilter) -> Unit, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxWidth()
                .windowCard(DeskShapes.Window)
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel(stringResource(R.string.devices_title_upper), Modifier.padding(horizontal = 16.dp))
            // Filter chips scroll sideways; the padding leaves room for their shadows
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeviceFilter.entries.forEach { f ->
                    NeuChipButton(
                        text = filterLabel(f, state.counts[f] ?: 0),
                        icon = null,
                        active = state.filter == f,
                        onClick = { onFilter(f) },
                        modifier = Modifier.padding(horizontal = 2.dp),
                        selected = state.filter == f,
                    )
                }
            }
            Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.rows.forEach { row -> DeviceListRow(row, onClick = { onOpen(row.id) }) }
                if (state.canAdd) {
                    NeuChipButton(
                        text = stringResource(R.string.device_add),
                        icon = Bi.PlusLg,
                        active = false,
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun filterLabel(filter: DeviceFilter, count: Int): String = when (filter) {
    DeviceFilter.ALL -> stringResource(R.string.filter_all, count)
    DeviceFilter.OFFLINE -> stringResource(R.string.filter_offline, count)
    DeviceFilter.DESK -> stringResource(R.string.room_desk)
    DeviceFilter.MEETING_ROOM -> stringResource(R.string.room_meeting)
    DeviceFilter.HALLWAY -> stringResource(R.string.room_hallway)
    DeviceFilter.ENTRANCE -> stringResource(R.string.room_entrance)
}

@Composable
private fun DeviceListRow(row: DeviceRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuRaised(DeskShapes.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(row.type.icon, active = row.online)
        Column(Modifier.weight(1f)) {
            Text(row.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                roomLabel(row.room) + " · " + summaryLabel(row.summary),
                fontSize = 11.sp,
                color = DeskColors.TextMuted,
                maxLines = 1,
            )
        }
        DeviceStatusPill(online = row.online, restarting = row.summary == DeviceSummary.Restarting)
    }
}

@Composable
private fun summaryLabel(summary: DeviceSummary): String = when (summary) {
    is DeviceSummary.Battery -> stringResource(R.string.summary_battery, summary.pct)
    is DeviceSummary.Firmware -> stringResource(R.string.summary_firmware, summary.version)
    DeviceSummary.Restarting -> stringResource(R.string.summary_restarting)
}
