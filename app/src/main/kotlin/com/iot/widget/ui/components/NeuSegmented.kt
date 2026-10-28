package com.iot.widget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens
import com.iot.widget.ui.theme.DeskShapes

/**
 * UI layer · segmented control (`.seg` / `.seg-btn`): equal-width chips, the selected one
 * pressed in with green text. Generic over the option key type T (an enum, a String...),
 * so every detail screen can reuse it.
 */
@Composable
fun <T> NeuSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label) ->
            val active = key == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = DeskDimens.MinTouch)
                    .then(
                        if (active) {
                            Modifier.neuInset(DeskShapes.Segment, distance = 3.dp, blur = 6.dp)
                        } else {
                            Modifier.neuRaised(DeskShapes.Segment, distance = 3.dp, blur = 6.dp)
                        },
                    )
                    .alpha(if (enabled) 1f else 0.5f)
                    .selectable(
                        selected = active,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(key) },
                    )
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) DeskColors.GreenText else DeskColors.Text,
                )
            }
        }
    }
}
