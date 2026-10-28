package com.iot.widget.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.domain.AudioOutput
import com.iot.widget.ui.components.DeviceRow
import com.iot.widget.ui.components.NeuSegmented
import com.iot.widget.ui.components.NeuSlider
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · Bluetooth details: paired devices, audio output and volume. The ACTIVE badge
 * follows the selected output (spec §9b), the mouse stays LINKED.
 */
@Composable
fun BluetoothDetail(
    state: DetailUiState,
    onOutput: (AudioOutput) -> Unit,
    onVolume: (Int) -> Unit,
    enabled: Boolean = true,
) {
    val output = state.settings.audioOutput
    DetailSection(stringResource(R.string.section_paired_devices)) {
        InsetBox {
            DeviceRow(Bi.SpeakerFill, stringResource(R.string.bt_desk_soundbar), stringResource(R.string.bt_desk_soundbar_sub)) {
                if (output == AudioOutput.Soundbar) {
                    Pill(stringResource(R.string.pill_active), PillStyle.On)
                } else {
                    Pill(stringResource(R.string.pill_paired), PillStyle.Neutral)
                }
            }
            DeviceRow(Bi.Headphones, stringResource(R.string.bt_headphones), stringResource(R.string.bt_headphones_sub)) {
                if (output == AudioOutput.Headphones) {
                    Pill(stringResource(R.string.pill_active), PillStyle.On)
                } else {
                    Pill(stringResource(R.string.pill_paired), PillStyle.Neutral)
                }
            }
            DeviceRow(Bi.Mouse2Fill, stringResource(R.string.bt_mouse), stringResource(R.string.bt_mouse_sub)) {
                Pill(stringResource(R.string.pill_linked), PillStyle.On)
            }
        }
    }
    DetailSection(stringResource(R.string.section_audio_output)) {
        NeuSegmented(
            options = listOf(
                AudioOutput.Soundbar to stringResource(R.string.output_soundbar),
                AudioOutput.Headphones to stringResource(R.string.output_headphones),
            ),
            selected = output,
            onSelect = onOutput,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
    DetailSection(stringResource(R.string.section_volume)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BiIcon(Bi.VolumeDown, tint = DeskColors.TextMuted, size = 14.sp)
            NeuSlider(state.settings.volume, onVolume, 0..100, Modifier.weight(1f), enabled = enabled)
            Text(
                "${state.settings.volume}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = DeskColors.TextMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp),
            )
        }
    }
}
