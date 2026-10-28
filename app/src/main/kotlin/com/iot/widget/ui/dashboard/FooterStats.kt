package com.iot.widget.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors

/** UI layer · the sensor footer (`.footer-stats`): temperature, humidity, power draw. */
@Composable
fun FooterStats(temperature: String, humidity: String, power: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Stat(Bi.ThermometerHalf, temperature, stringResource(R.string.footer_temperature))
        Stat(Bi.Droplet, humidity, stringResource(R.string.footer_humidity))
        Stat(Bi.LightningCharge, power, stringResource(R.string.footer_power))
    }
}

@Composable
private fun Stat(icon: Bi, value: String, label: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label $value" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BiIcon(icon, tint = DeskColors.TextMuted, size = 11.sp)
        Text(value, fontSize = 11.sp, color = DeskColors.TextMuted)
    }
}
