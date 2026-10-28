package com.iot.widget.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.devices.Room
import com.iot.widget.ui.components.CloseWhen
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.TextFieldNeu
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens
import com.iot.widget.ui.users.messageRes
import com.iot.widget.ui.users.relativeLabel
import java.util.Locale

/** Callbacks of the device page, bundled so previews and tests can pass [NONE]. */
data class DeviceDetailActions(
    val onBack: () -> Unit,
    val onStartRename: () -> Unit,
    val onNameDraft: (String) -> Unit,
    val onSaveName: () -> Unit,
    val onCancelRename: () -> Unit,
    val onRoom: (Room) -> Unit,
    val onRestart: () -> Unit,
    val onDelete: () -> Unit,
    val onViewLogs: () -> Unit = {},
) {
    companion object {
        val NONE = DeviceDetailActions({}, {}, {}, {}, {}, {}, {}, {})
    }
}

/** UI layer · one device (spec §6.8). Closes itself after a successful delete. */
@Composable
fun DeviceDetailScreen(onBack: () -> Unit, onViewLogs: (String) -> Unit = {}, viewModel: DeviceDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CloseWhen(state.deleted, onBack)
    DeviceDetailContent(
        state = state,
        actions = DeviceDetailActions(
            onBack = onBack,
            onStartRename = viewModel::startRename,
            onNameDraft = viewModel::setNameDraft,
            onSaveName = viewModel::saveName,
            onCancelRename = viewModel::cancelRename,
            onRoom = viewModel::setRoom,
            onRestart = viewModel::restart,
            onDelete = viewModel::delete,
            onViewLogs = { state.device?.id?.let(onViewLogs) },
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailContent(state: DeviceDetailUiState, actions: DeviceDetailActions) {
    val device = state.device
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    // The device name is user data, so it is uppercased here (Locale.ROOT: no Turkish "İ")
    SubPage(
        title = device?.name.orEmpty().uppercase(Locale.ROOT),
        icon = device?.type?.icon ?: Bi.Cpu,
        onBack = actions.onBack,
    ) {
        if (device == null) {
            if (state.loaded) Text(stringResource(R.string.device_not_found), fontSize = 13.sp, color = DeskColors.TextMuted)
            return@SubPage
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeviceStatusPill(online = device.online, restarting = state.restarting)
            Text(stringResource(R.string.device_last_seen, relativeLabel(state.lastSeen)), fontSize = 11.sp, color = DeskColors.TextMuted)
        }
        DetailSection(stringResource(R.string.device_info_upper)) {
            InsetBox {
                InfoRow(stringResource(R.string.info_type), typeLabel(device.type))
                InfoRow(stringResource(R.string.info_room), roomLabel(device.room))
                InfoRow(stringResource(R.string.info_firmware), device.info.firmware)
                InfoRow(stringResource(R.string.info_ip), device.info.ipAddress)
                InfoRow(stringResource(R.string.info_mac), device.info.macAddress)
                InfoRow(
                    stringResource(R.string.info_power),
                    device.info.batteryPct?.let { stringResource(R.string.summary_battery, it) } ?: stringResource(R.string.info_mains),
                )
                InfoRow(stringResource(R.string.info_signal), stringResource(R.string.info_dbm, device.info.signalDbm))
            }
        }
        if (state.canViewLogs) {
            NeuChipButton(stringResource(R.string.device_view_logs), Bi.JournalText, active = false, onClick = actions.onViewLogs, modifier = Modifier.fillMaxWidth())
        }
        if (state.canEdit) {
            DetailSection(stringResource(R.string.field_name_upper)) {
                val draft = state.nameDraft
                if (draft == null) {
                    NeuChipButton(stringResource(R.string.device_rename), Bi.Pencil, active = false, onClick = actions.onStartRename, modifier = Modifier.fillMaxWidth())
                } else {
                    TextFieldNeu(value = draft, onValueChange = actions.onNameDraft, label = stringResource(R.string.field_name))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeuChipButton(stringResource(R.string.action_save), null, active = true, onClick = actions.onSaveName, modifier = Modifier.weight(1f))
                        NeuChipButton(stringResource(R.string.action_cancel), null, active = false, onClick = actions.onCancelRename, modifier = Modifier.weight(1f))
                    }
                    state.error?.let { Text(stringResource(it.messageRes()), fontSize = 12.sp, color = DeskColors.Red) }
                }
            }
            DetailSection(stringResource(R.string.info_room_upper)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Room.entries.forEach { r ->
                        NeuChipButton(roomLabel(r), null, active = device.room == r, onClick = { actions.onRoom(r) }, modifier = Modifier.padding(2.dp), selected = device.room == r)
                    }
                }
            }
            NeuChipButton(
                stringResource(R.string.device_restart),
                Bi.ArrowRepeat,
                active = false,
                onClick = actions.onRestart,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.restarting,
            )
        }
        if (state.canDelete) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DeskDimens.MinTouch)
                    .clickable(role = SemanticsRole.Button) { confirmDelete = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BiIcon(Bi.Trash, tint = DeskColors.Red, size = 14.sp)
                Text(stringResource(R.string.device_delete), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeskColors.Red)
            }
        }
    }
    if (confirmDelete && device != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DeskColors.Bg,
            title = { Text(stringResource(R.string.delete_confirm, device.name), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; actions.onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = DeskColors.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel), color = DeskColors.TextMuted) }
            },
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = DeskColors.TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeskColors.LogText)
    }
}
