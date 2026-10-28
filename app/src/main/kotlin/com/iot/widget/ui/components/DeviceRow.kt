package com.iot.widget.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · the rounded icon tile (`.icon-box`). Raised and grey normally; pressed in with a
 * green icon when its device is on (`.switch-holder.is-on .icon-box`).
 */
@Composable
fun IconBox(
    icon: Bi,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    boxSize: Dp = 32.dp,
    iconSize: TextUnit = 16.sp,
    corner: Dp = 10.dp,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .size(boxSize)
            .then(
                if (active) {
                    Modifier.neuInset(shape, distance = 2.dp, blur = 4.dp, dark = DeskColors.ShadowDark)
                } else {
                    Modifier.neuRaised(shape, distance = 3.dp, blur = 6.dp)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        BiIcon(icon, tint = if (active) DeskColors.Green else DeskColors.IconDefault, size = iconSize)
    }
}

/** UI layer · one line of a detail list (`.device-row`): icon tile, title + subtitle, trailing badge. */
@Composable
fun DeviceRow(
    icon: Bi,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBox(icon, boxSize = 28.dp, iconSize = 14.sp, corner = 9.dp)
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 11.sp, color = DeskColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** UI layer · uppercase section heading (`.section-label`). Pass text already uppercase (strings.xml). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = DeskColors.TextMuted,
        letterSpacing = 0.4.sp,
    )
}
