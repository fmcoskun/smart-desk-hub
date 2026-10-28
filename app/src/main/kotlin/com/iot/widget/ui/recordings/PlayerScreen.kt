package com.iot.widget.ui.recordings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import com.iot.widget.R
import com.iot.widget.domain.cameras.Playback
import com.iot.widget.ui.cameras.VideoSurface
import com.iot.widget.ui.cameras.rememberClipPlayer
import com.iot.widget.ui.components.NeuSegmented
import com.iot.widget.ui.components.NeuSlider
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * UI layer · plays one recording (spec §6.7). The ExoPlayer is the source of truth: play/pause
 * arrives through a Player.Listener, and the position is copied into Compose state four times
 * a second while it plays (nothing is polled while paused).
 */
@Composable
fun PlayerScreen(onBack: () -> Unit, viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = state.info
    if (info == null) {
        // still loading, not allowed, or the recording is gone
        SubPage(title = stringResource(R.string.recordings_title_upper), icon = Bi.Film, onBack = onBack) {
            when {
                state.loaded && !state.allowed -> Text(stringResource(R.string.recordings_not_allowed), fontSize = 13.sp, color = DeskColors.TextMuted)
                state.notFound -> Text(stringResource(R.string.recording_not_found), fontSize = 13.sp, color = DeskColors.TextMuted)
            }
        }
        return
    }
    val memory = rememberPlaybackMemory()
    // null while the app is in the background (the player is released then)
    val player = rememberClipPlayer(info.clip, loop = false, playWhenReady = memory.playing)
    if (player != null) {
        DisposableEffect(player) {
            // a new player (rotation, back from the background): put it where the old one was
            player.seekTo(memory.positionMs)
            player.setPlaybackSpeed(memory.speed)
            // play/pause comes from the player itself, not from polling
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    memory.playing = isPlaying || (player.playWhenReady && player.playbackState != Player.STATE_ENDED)
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
        // the position only moves while playing: poll four times a second then, sleep otherwise
        // repeatOnLifecycle: the loop stops at ON_STOP (the player is released then) and
        // starts again at ON_START, instead of polling a released player in the background
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(player, memory.playing, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (memory.playing) {
                    memory.positionMs = player.currentPosition
                    delay(250)
                }
                memory.positionMs = player.currentPosition
            }
        }
    }
    PlayerContent(
        info = info,
        position = memory.positionMs,
        playing = memory.playing,
        speed = memory.speed,
        onBack = onBack,
        onPlayPause = {
            player?.let {
                if (it.isPlaying) {
                    it.pause()
                    memory.playing = false
                } else {
                    // at the end, "play" starts the clip again
                    if (it.playbackState == Player.STATE_ENDED) it.seekTo(0)
                    it.play()
                    memory.playing = true
                }
            }
        },
        onSeek = { player?.seekTo(it); memory.positionMs = it },
        onSkip = { delta ->
            val to = Playback.skip(player?.currentPosition ?: memory.positionMs, delta, info.durationMs)
            player?.seekTo(to)
            memory.positionMs = to
        },
        onSpeed = { memory.speed = it; player?.setPlaybackSpeed(it) },
    ) { modifier -> player?.let { VideoSurface(it, modifier) } }
}

@Composable
fun PlayerContent(
    info: PlayerInfoUi,
    position: Long,
    playing: Boolean,
    speed: Float,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    onSpeed: (Float) -> Unit,
    video: @Composable (Modifier) -> Unit,
) {
    SubPage(title = info.camera.uppercase(Locale.ROOT), icon = Bi.Film, onBack = onBack) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2C3036)),
        ) { video(Modifier.matchParentSize()) }
        // while dragging, the thumb follows the finger and nothing is decoded; one seek on release
        var dragTo by remember { mutableStateOf<Int?>(null) }
        NeuSlider(
            value = dragTo ?: position.coerceAtMost(info.durationMs).toInt(),
            onValueChange = { dragTo = it },
            valueRange = 0..info.durationMs.toInt(),
            modifier = Modifier.fillMaxWidth(),
            onValueChangeFinished = {
                dragTo?.let { onSeek(it.toLong()) }
                dragTo = null
            },
        )
        Text(
            stringResource(R.string.player_position, Playback.formatDuration(position), Playback.formatDuration(info.durationMs)),
            fontFamily = JetBrainsMonoFamily,
            fontSize = 12.sp,
            color = DeskColors.TextMuted,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Bi.ArrowCounterclockwise, stringResource(R.string.player_back10), 52.dp) { onSkip(-Playback.SKIP_MS) }
            RoundButton(
                if (playing) Bi.PauseFill else Bi.PlayFill,
                stringResource(if (playing) R.string.player_pause else R.string.player_play),
                64.dp,
                onClick = onPlayPause,
            )
            RoundButton(Bi.ArrowClockwise, stringResource(R.string.player_fwd10), 52.dp) { onSkip(Playback.SKIP_MS) }
        }
        NeuSegmented(listOf(1f to stringResource(R.string.speed_1x), 2f to stringResource(R.string.speed_2x)), selected = speed, onSelect = onSpeed, modifier = Modifier.fillMaxWidth())
        DetailSection(stringResource(R.string.recording_info_upper)) {
            InsetBox {
                Field(stringResource(R.string.info_camera), info.camera)
                Field(stringResource(R.string.info_started), info.started)
                Field(stringResource(R.string.info_duration), Playback.formatDuration(info.durationMs))
                Field(stringResource(R.string.info_event), info.event.name)
            }
        }
    }
}

/** Raised round icon button; [label] is read out by TalkBack (the button has no text). */
@Composable
private fun RoundButton(icon: Bi, label: String, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .neuRaised(CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        BiIcon(icon, tint = DeskColors.Text, size = (size.value * 0.36f).sp)
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, fontSize = 12.sp, color = DeskColors.TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DeskColors.LogText)
    }
}
