package com.iot.widget.domain.cameras

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipsTest {

    @Test
    fun catalogCoversEveryCameraAndExactDurations() {
        assertEquals(setOf("cam_desk", "cam_door", "cam_meeting", "cam_hallway"), Clips.LIVE.keys)
        // two recording clips per camera, filmed in that camera's colours; lengths as cut by ffmpeg
        assertEquals(
            mapOf(
                "rec_desk_15" to 15_000L, "rec_desk_45" to 45_000L,
                "rec_door_30" to 30_000L, "rec_door_60" to 60_000L,
                "rec_meeting_15" to 15_000L, "rec_meeting_60" to 60_000L,
                "rec_hallway_30" to 30_000L, "rec_hallway_45" to 45_000L,
            ),
            Clips.RECORDINGS,
        )
        assertEquals(Clips.LIVE.keys, Clips.RECORDINGS_BY_CAMERA.keys)
        assertEquals(Clips.RECORDINGS.keys, Clips.RECORDINGS_BY_CAMERA.values.flatten().toSet())
    }
}
