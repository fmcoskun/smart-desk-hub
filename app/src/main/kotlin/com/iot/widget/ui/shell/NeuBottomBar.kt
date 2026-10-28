package com.iot.widget.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.components.neuInset
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens

/** One bottom-bar tab: what it shows and whether it is the current one. */
data class BottomTab(val label: String, val icon: Bi, val selected: Boolean, val onClick: () -> Unit)

/**
 * UI layer · neumorphic bottom navigation (spec §5): a raised bar, the current tab pressed in
 * with green text — the same language as the scene chips.
 */
@Composable
fun NeuBottomBar(tabs: List<BottomTab>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .neuRaised(RoundedCornerShape(20.dp))
            .padding(6.dp)
            // one group of tabs: TalkBack says "tab 2 of 5"
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { tab ->
            val color = if (tab.selected) DeskColors.GreenText else DeskColors.TextMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = DeskDimens.MinTouch)
                    .then(if (tab.selected) Modifier.neuInset(RoundedCornerShape(14.dp), distance = 2.dp, blur = 5.dp) else Modifier)
                    .selectable(selected = tab.selected, role = Role.Tab, onClick = tab.onClick)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BiIcon(tab.icon, tint = color, size = 16.sp)
                Text(tab.label, fontSize = 10.sp, fontWeight = if (tab.selected) FontWeight.Bold else FontWeight.Medium, color = color)
            }
        }
    }
}
