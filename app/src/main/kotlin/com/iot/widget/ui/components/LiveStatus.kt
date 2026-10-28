package com.iot.widget.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · a status line that changes by itself ("Searching nearby…" → "Found a new device").
 * A live region makes TalkBack announce each new text without the user moving focus to it.
 */
@Composable
fun LiveStatus(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 14.sp,
        color = DeskColors.TextMuted,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
