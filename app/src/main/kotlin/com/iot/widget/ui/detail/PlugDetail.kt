package com.iot.widget.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.domain.PlugTimer
import com.iot.widget.ui.components.DeviceRow
import com.iot.widget.ui.components.NeuSegmented
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors

/** Hourly bar heights of the reference's "Energy Today" chart, in percent. */
private val ENERGY_BARS = listOf(35, 55, 80, 95, 70, 85, 60, 40)

/** UI layer · Smart Plug details: today's energy, the auto-off timer and the outlets. */
@Composable
fun PlugDetail(
    state: DetailUiState,
    onTimer: (PlugTimer) -> Unit,
    exactAllowed: Boolean = true,
    onAllowExact: () -> Unit = {},
    enabled: Boolean = true,
) {
    DetailSection(stringResource(R.string.section_energy_today)) {
        InsetBox {
            // alignByBaseline on both texts = CSS `align-items: baseline` (big number and unit share a baseline)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.energy_value),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    stringResource(R.string.energy_unit),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DeskColors.TextMuted,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            EnergyChart()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("09:00", "13:00", "17:00").forEach { Text(it, fontSize = 10.sp, color = DeskColors.TextMuted) }
            }
        }
    }
    DetailSection(stringResource(R.string.section_auto_off_timer)) {
        NeuSegmented(
            options = listOf(
                PlugTimer.Off to stringResource(R.string.timer_off),
                PlugTimer.M30 to stringResource(R.string.timer_30m),
                PlugTimer.H1 to stringResource(R.string.timer_1h),
                PlugTimer.H2 to stringResource(R.string.timer_2h),
            ),
            selected = state.settings.plugTimer,
            onSelect = onTimer,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
        if (enabled && !exactAllowed) ExactTimingNotice(onAllowExact)
    }
    DetailSection(stringResource(R.string.section_outlets)) {
        InsetBox {
            DeviceRow(Bi.Display, stringResource(R.string.outlet_monitors), stringResource(R.string.outlet_monitors_sub)) {
                Pill(stringResource(R.string.pill_on), PillStyle.On)
            }
            DeviceRow(Bi.UsbPlugFill, stringResource(R.string.outlet_dock), stringResource(R.string.outlet_dock_sub)) {
                Pill(stringResource(R.string.pill_on), PillStyle.On)
            }
        }
    }
}

/** Eight green bars with a soft glow (`.bar-chart span`). */
@Composable
private fun EnergyChart() {
    val shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ENERGY_BARS.forEach { percent ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(percent / 100f)
                    .dropShadow(shape, Shadow(radius = 4.dp, color = DeskColors.GreenGlow, offset = DpOffset(0.dp, 2.dp)))
                    .background(Brush.verticalGradient(listOf(Color(0xFF5ED98A), DeskColors.Green)), shape),
            )
        }
    }
}
