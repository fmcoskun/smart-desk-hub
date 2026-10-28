package com.iot.widget.ui.cameras

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.JetBrainsMonoFamily

/**
 * UI layer · the picture area of a camera (video, Privacy Guard blackout or "Offline", LIVE badge, clock).
 * Picture background while the video is loading.
 */
private val Screen = Color(0xFF2C3036)
private val OfflineScreen = Color(0xFFD5D5D5)

/**
 * UI layer · the picture area of a camera: the video (through the [video] slot), the Privacy
 * Guard blackout or "Offline", plus the LIVE badge and clock. Used by the grid (small) and the
 * full-screen page (large); [scale] grows the overlay text with it.
 */
@Composable
fun CameraPicture(
    tile: CameraTileUi,
    clock: String,
    video: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.clip(shape).background(if (tile.feed == FeedState.OFFLINE) OfflineScreen else Screen)) {
        when (tile.feed) {
            FeedState.LIVE -> {
                video(Modifier.fillMaxSize())
                LiveOverlay(clock, (9 * scale).sp)
            }
            FeedState.PRIVACY -> Notice(Bi.ShieldLockFill, stringResource(R.string.camera_privacy_off), Color.White, scale)
            FeedState.OFFLINE -> Notice(Bi.CameraVideoOffFill, stringResource(R.string.camera_offline), DeskColors.TextMuted, scale)
        }
    }
}

@Composable
private fun BoxScope.LiveOverlay(clock: String, size: TextUnit) {
    Row(
        Modifier
            .align(Alignment.TopStart)
            .padding(6.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.camera_live_dot), color = DeskColors.Red, fontSize = size)
        Text(stringResource(R.string.camera_live), color = Color.White, fontSize = size, fontWeight = FontWeight.Bold)
    }
    Text(
        clock,
        Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        color = Color.White,
        fontSize = size,
        fontFamily = JetBrainsMonoFamily,
    )
}

@Composable
private fun BoxScope.Notice(icon: Bi, text: String, color: Color, scale: Float) {
    Column(
        Modifier
            .align(Alignment.Center)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BiIcon(icon, tint = color, size = (20 * scale).sp)
        Text(text, color = color, fontSize = (11 * scale).sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}
