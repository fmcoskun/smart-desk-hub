package com.iot.widget.ui.recordings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * UI layer · where the user was in a recording: position, speed, playing or paused.
 *
 * Rotating the phone destroys and recreates the screen, and with it the ExoPlayer. Plain
 * `remember` would forget everything (the clip would restart at 0:00, 1×). `rememberSaveable`
 * writes these values into the saved-instance Bundle and gives them back to the new screen,
 * which then puts the new player where the old one was.
 */
class PlaybackMemory(positionMs: Long = 0L, speed: Float = 1f, playing: Boolean = true) {
    var positionMs by mutableLongStateOf(positionMs)
    var speed by mutableFloatStateOf(speed)
    var playing by mutableStateOf(playing)

    companion object {
        /** How to turn it into Bundle-friendly values and back. */
        val Saver = listSaver<PlaybackMemory, Any>(
            save = { listOf(it.positionMs, it.speed, it.playing) },
            restore = { PlaybackMemory(it[0] as Long, it[1] as Float, it[2] as Boolean) },
        )
    }
}

@Composable
fun rememberPlaybackMemory(): PlaybackMemory = rememberSaveable(saver = PlaybackMemory.Saver) { PlaybackMemory() }
