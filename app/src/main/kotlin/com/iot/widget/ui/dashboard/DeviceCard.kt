package com.iot.widget.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.MAX_BRIGHTNESS
import com.iot.widget.domain.MIN_BRIGHTNESS
import com.iot.widget.ui.components.IconBox
import com.iot.widget.ui.components.NeuSlider
import com.iot.widget.ui.components.NeuSwitch
import com.iot.widget.ui.components.switchHolder
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/** Icon of each dashboard device (same glyphs as the reference cards). */
val DeviceId.icon: Bi
    get() = when (this) {
        DeviceId.Soundbar -> Bi.Bluetooth
        DeviceId.DeskLight -> Bi.LightbulbFill
        DeviceId.SmartPlug -> Bi.Outlet
        DeviceId.PrivacyGuard -> Bi.ShieldLockFill
    }

/**
 * UI layer · one device card (`.switch-holder`). Tapping the card body opens its detail screen;
 * the switch and the dimmer handle their own touches, so tapping them does not open it.
 *
 * `dimmer` is a slot: only the LED card passes one (the recessed brightness slider).
 */
@Composable
fun DeviceCard(
    card: DeviceCardUi,
    /** null = view only: the switch is shown disabled. */
    onToggle: ((Boolean) -> Unit)?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    dimmer: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .switchHolder(DeskShapes.Card)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBox(card.id.icon, active = card.isOn)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(card.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = when {
                        !card.online -> stringResource(R.string.status_offline_card)
                        card.timerMinutesLeft != null ->
                            pluralStringResource(R.plurals.plug_off_in, card.timerMinutesLeft, card.timerMinutesLeft)
                        card.timerOverdue -> stringResource(R.string.plug_turning_off)
                        card.statusRes != null -> stringResource(card.statusRes)
                        else -> card.subtitle
                    },
                    fontSize = 11.sp,
                    color = when {
                        !card.online -> DeskColors.Red
                        card.timerMinutesLeft != null || card.timerOverdue -> DeskColors.Green
                        else -> DeskColors.TextMuted
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            NeuSwitch(checked = card.isOn, onCheckedChange = onToggle, contentDescription = card.name)
        }
        dimmer?.invoke()
    }
}

/** The LED card's recessed dimmer row; faded and inactive while the LED is off. */
@Composable
fun DimmerRow(brightness: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BiIcon(Bi.Sun, tint = DeskColors.TextMuted, size = 13.sp)
        NeuSlider(
            value = brightness,
            onValueChange = onChange,
            valueRange = MIN_BRIGHTNESS..MAX_BRIGHTNESS,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$brightness%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = DeskColors.TextMuted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(34.dp),
        )
    }
}
