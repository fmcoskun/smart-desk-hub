package com.iot.widget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.SmartDeskTheme

/**
 * UI layer · design-time preview of every component. @Preview functions are rendered by
 * Android Studio's preview pane only; they never run in the app.
 */
@Preview(widthDp = 360, heightDp = 420)
@Composable
private fun ComponentGalleryPreview() {
    SmartDeskTheme {
        Column(
            Modifier
                .background(DeskColors.Bg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuChipButton("Work", Bi.BriefcaseFill, active = true, onClick = {}, modifier = Modifier.weight(1f))
                NeuChipButton("Meet", Bi.CameraVideoFill, active = false, onClick = {}, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuSwitch(checked = true, onCheckedChange = {}, contentDescription = "on")
                NeuSwitch(checked = false, onCheckedChange = {}, contentDescription = "off")
                IconBox(Bi.Bluetooth, active = true)
            }
            NeuSlider(value = 80, onValueChange = {}, valueRange = 5..100)
            NeuSegmented(listOf(1 to "Warm", 2 to "Neutral", 3 to "Cool"), selected = 2, onSelect = {})
            DeviceRow(Bi.SpeakerFill, "Desk Soundbar", "A2DP · AAC") { Pill("ACTIVE", PillStyle.On) }
            SectionLabel("PAIRED DEVICES")
        }
    }
}
