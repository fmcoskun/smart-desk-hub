package com.iot.widget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · the ON/OFF switch of the reference (`.switch-toggle`): a 96×44 recessed track with
 * a 42×24 knob that slides from x=7 to x=47 and reads OFF/ON.
 *
 * State hoisting: the switch does not remember whether it is on. The caller passes `checked`
 * and gets `onCheckedChange` back; the single source of truth stays in the ViewModel.
 * Passing `onCheckedChange = null` renders it disabled (e.g. for a Guest user later).
 */
@Composable
fun NeuSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val track = RoundedCornerShape(22.dp)
    val knob = RoundedCornerShape(20.dp)
    // animate*AsState: Compose re-runs this function when `checked` changes and animates
    // from the old value to the new one over 300 ms (the CSS transition)
    val knobX by animateDpAsState(
        targetValue = if (checked) 47.dp else 7.dp,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "knob",
    )
    Box(
        modifier = modifier
            .size(width = 96.dp, height = 44.dp)
            .alpha(if (onCheckedChange == null) 0.4f else 1f)
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                enabled = onCheckedChange != null,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
            .neuInset(
                shape = track,
                distance = 9.dp,
                blur = 12.dp,
                dark = Color(0x40000000),
                light = Color(0x99FFFFFF),
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX, y = 10.dp)
                .size(width = 42.dp, height = 24.dp)
                .neuRaised(
                    shape = knob,
                    distance = 3.dp,
                    blur = 5.dp,
                    light = Color(0x80FFFFFF),
                    dark = if (checked) DeskColors.GreenGlow else Color(0x40000000),
                    background = if (checked) DeskColors.Green else DeskColors.KnobOff,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (checked) R.string.switch_on else R.string.switch_off),
                color = if (checked) Color.White else DeskColors.KnobOffText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
