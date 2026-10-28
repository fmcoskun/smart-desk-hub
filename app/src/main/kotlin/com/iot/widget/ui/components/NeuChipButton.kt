package com.iot.widget.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens
import com.iot.widget.ui.theme.DeskShapes

/** UI layer · scene chip (`.scene-chip`): raised, or pressed in with green text when active. */
@Composable
fun NeuChipButton(
    text: String,
    icon: Bi?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Overrides the text colour (e.g. red for an "Error" filter); null = the usual green/dark. */
    contentColor: Color? = null,
    /**
     * For filter/choice chips: announced to TalkBack as selected or not. Null (the default) for
     * action buttons, whose green "active" look is just the primary style (Save, Continue).
     */
    selected: Boolean? = null,
) {
    val color = contentColor ?: if (active) DeskColors.GreenText else DeskColors.Text
    Row(
        modifier = modifier
            .heightIn(min = DeskDimens.MinTouch)
            .then(
                if (active) {
                    Modifier.neuInset(DeskShapes.Chip, distance = 3.dp, blur = 6.dp)
                } else {
                    Modifier.neuRaised(DeskShapes.Chip, distance = 4.dp, blur = 8.dp)
                },
            )
            // faded when disabled; alpha after the shadows so they are not clipped
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (selected != null) this.selected = selected }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) BiIcon(icon, tint = color, size = 13.sp)
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
        )
    }
}
