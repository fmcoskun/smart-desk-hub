package com.iot.widget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.theme.DeskColors

/** Neutral = grey, On = green, Off = red (`.pill`, `.pill.on`, `.pill.off`). */
enum class PillStyle { Neutral, On, Off }

/** UI layer · small status badge such as ACTIVE, PAIRED, BLOCKED. */
@Composable
fun Pill(text: String, style: PillStyle, modifier: Modifier = Modifier) {
    val (fg, bg) = when (style) {
        PillStyle.Neutral -> DeskColors.TextMuted to Color(0x0F000000)
        PillStyle.On -> DeskColors.Green to Color(0x1F00B33C)
        PillStyle.Off -> DeskColors.Red to Color(0x1AE63946)
    }
    Text(
        text = text,
        color = fg,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(bg, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
