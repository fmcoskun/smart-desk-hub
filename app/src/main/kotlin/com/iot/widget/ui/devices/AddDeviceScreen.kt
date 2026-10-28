package com.iot.widget.ui.devices

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.devices.PAIRABLE_TYPES
import com.iot.widget.domain.devices.PairingStep
import com.iot.widget.domain.devices.Room
import com.iot.widget.ui.components.CloseWhen
import com.iot.widget.ui.components.IconBox
import com.iot.widget.ui.components.LiveStatus
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.TextFieldNeu
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes
import com.iot.widget.ui.users.messageRes

/** UI layer · "Add device": one screen per pairing step, closes when the device is added. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddDeviceScreen(onDone: () -> Unit, viewModel: AddDeviceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CloseWhen(state.step == PairingStep.Done, onDone)
    SubPage(title = stringResource(R.string.add_device_title_upper), icon = Bi.PlusLg, onBack = onDone) {
        when (val step = state.step) {
            PairingStep.ChooseType -> {
                Text(stringResource(R.string.add_choose_type), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                PAIRABLE_TYPES.forEach { type ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .neuRaised(DeskShapes.Card)
                            .clickable(role = SemanticsRole.Button) { viewModel.chooseType(type) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBox(type.icon)
                        Text(typeLabel(type), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        BiIcon(Bi.ChevronRight, tint = DeskColors.TextMuted, size = 13.sp)
                    }
                }
            }
            is PairingStep.Searching -> Searching()
            is PairingStep.Found -> {
                Centered(Bi.CheckCircleFill, DeskColors.Green, stringResource(R.string.add_found))
                Text(step.suggestedName, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                NeuChipButton(stringResource(R.string.add_continue), null, active = true, onClick = viewModel::continueToNaming, modifier = Modifier.fillMaxWidth())
            }
            is PairingStep.Naming -> {
                DetailSection(stringResource(R.string.field_name_upper)) {
                    TextFieldNeu(value = step.name, onValueChange = viewModel::setName, label = stringResource(R.string.field_name))
                }
                DetailSection(stringResource(R.string.info_room_upper)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Room.entries.forEach { r ->
                            NeuChipButton(roomLabel(r), null, active = step.room == r, onClick = { viewModel.setRoom(r) }, modifier = Modifier.padding(2.dp), selected = step.room == r)
                        }
                    }
                }
                state.error?.let { Text(stringResource(it.messageRes()), fontSize = 12.sp, color = DeskColors.Red) }
                NeuChipButton(stringResource(R.string.add_confirm), Bi.PlusLg, active = true, onClick = viewModel::add, modifier = Modifier.fillMaxWidth())
            }
            PairingStep.Done -> Unit
        }
    }
}

/** A pulsing search icon while the (simulated) scan runs. */
@Composable
private fun Searching() {
    val pulse = rememberInfiniteTransition(label = "search")
    val scale by pulse.animateFloat(1f, 1.2f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "scale")
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BiIcon(Bi.Search, tint = DeskColors.Green, size = 36.sp, modifier = Modifier.scale(scale))
        LiveStatus(stringResource(R.string.add_searching), Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun Centered(icon: Bi, tint: androidx.compose.ui.graphics.Color, text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BiIcon(icon, tint = tint, size = 36.sp)
        LiveStatus(text, Modifier.padding(top = 10.dp))
    }
}
