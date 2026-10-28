package com.iot.widget.domain.cameras

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTest {

    @Test
    fun skipClampsToClipBounds() {
        assertEquals(0L, Playback.skip(4_000, -Playback.SKIP_MS, 60_000))
        assertEquals(60_000L, Playback.skip(55_000, Playback.SKIP_MS, 60_000))
        assertEquals(30_000L, Playback.skip(20_000, Playback.SKIP_MS, 60_000))
        assertEquals(0L, Playback.skip(0, Playback.SKIP_MS, 0)) // duration not known yet
    }

    @Test
    fun formatsDurations() {
        assertEquals("0:00", Playback.formatDuration(0))
        assertEquals("0:45", Playback.formatDuration(45_000))
        assertEquals("1:00", Playback.formatDuration(60_000))
        assertEquals("12:05", Playback.formatDuration(725_000))
        assertEquals("0:09", Playback.formatDuration(9_999)) // whole seconds, rounded down
    }
}
