package com.iot.widget.ui.cameras

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.iot.widget.R

/** Raw clip name (see `Clips`) → resource id. An explicit map, so the shrinker keeps the files. */
private val RAW = mapOf(
    "live_desk" to R.raw.live_desk,
    "live_door" to R.raw.live_door,
    "live_meeting" to R.raw.live_meeting,
    "live_hallway" to R.raw.live_hallway,
    "rec_desk_15" to R.raw.rec_desk_15,
    "rec_desk_45" to R.raw.rec_desk_45,
    "rec_door_30" to R.raw.rec_door_30,
    "rec_door_60" to R.raw.rec_door_60,
    "rec_meeting_15" to R.raw.rec_meeting_15,
    "rec_meeting_60" to R.raw.rec_meeting_60,
    "rec_hallway_30" to R.raw.rec_hallway_30,
    "rec_hallway_45" to R.raw.rec_hallway_45,
)

/**
 * Decoder choice: software decoders first, hardware as fallback. The clips are tiny
 * (640×360, 15 fps), so software decoding is cheap, and four tiles never compete for the few
 * hardware decoder instances a phone has. (It also sidesteps the emulator's broken goldfish
 * H.264 decoder, which fails on the first buffer.)
 */
@OptIn(UnstableApi::class)
private val SoftwareFirst = MediaCodecSelector { mime, secure, tunneling ->
    MediaCodecUtil.getDecoderInfos(mime, secure, tunneling).sortedBy { it.hardwareAccelerated }
}

/**
 * UI layer · an ExoPlayer for one clip, tied to this screen's lifetime (the Media3 player
 * lifecycle, spec §8). Returns null while there is no player (app in the background).
 *  - ON_START creates the player, ON_STOP releases it: a backgrounded app holds no decoder,
 *    buffers or playback thread. Forgetting release() is the classic player leak.
 *  - Leaving the screen (dispose) releases it too.
 *  - [playWhenReady] is read when the player is (re)created, so a recording paused before the
 *    app went to the background stays paused when it comes back.
 * The player lives in Compose state: a new player means a recomposition that hands it to the
 * video surface. Muted: demo cameras have no sound.
 */
@OptIn(UnstableApi::class)
@Composable
fun rememberClipPlayer(clip: String, loop: Boolean, playWhenReady: Boolean = true): ExoPlayer? {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val autoPlay by rememberUpdatedState(playWhenReady)
    var player by remember(clip) { mutableStateOf<ExoPlayer?>(null) }
    DisposableEffect(clip, lifecycle) {
        fun create() {
            if (player != null) return
            player = ExoPlayer.Builder(context, DefaultRenderersFactory(context).setMediaCodecSelector(SoftwareFirst)).build().apply {
                // android.resource://<package>/<id>: ExoPlayer reads raw resources through this URI
                setMediaItem(MediaItem.fromUri("android.resource://${context.packageName}/${RAW.getValue(clip)}"))
                repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                volume = 0f
                this.playWhenReady = autoPlay
                prepare()
            }
        }
        fun release() {
            player?.release()
            player = null
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> create()
                Lifecycle.Event.ON_STOP -> release()
                else -> Unit
            }
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) create()
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            release()
        }
    }
    return player
}

/**
 * Draws the player's video. TextureView (not SurfaceView) because it is clipped by the
 * rounded tile corners like any other view.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoSurface(player: Player, modifier: Modifier = Modifier) {
    PlayerSurface(player = player, modifier = modifier, surfaceType = SURFACE_TYPE_TEXTURE_VIEW)
}
