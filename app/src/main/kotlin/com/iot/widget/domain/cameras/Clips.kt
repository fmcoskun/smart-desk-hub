package com.iot.widget.domain.cameras

/**
 * Domain layer · the demo video clips in `res/raw` (made by `tools/make_clips.sh`).
 * Names are raw-resource names; the UI turns them into `android.resource://` URIs.
 */
object Clips {
    /** Camera device id → its looping "live" feed. */
    val LIVE: Map<String, String> = mapOf(
        "cam_desk" to "live_desk",
        "cam_door" to "live_door",
        "cam_meeting" to "live_meeting",
        "cam_hallway" to "live_hallway",
    )

    /**
     * Recording clip → its exact length. The Recordings list shows these durations, so they
     * must match the files (the script cuts them with `-t`; check with ffprobe).
     */
    val RECORDINGS: Map<String, Long> = mapOf(
        "rec_desk_15" to 15_000L,
        "rec_desk_45" to 45_000L,
        "rec_door_30" to 30_000L,
        "rec_door_60" to 60_000L,
        "rec_meeting_15" to 15_000L,
        "rec_meeting_60" to 60_000L,
        "rec_hallway_30" to 30_000L,
        "rec_hallway_45" to 45_000L,
    )

    /** Camera → its recording clips (filmed in that camera's colours, like its live feed). */
    val RECORDINGS_BY_CAMERA: Map<String, List<String>> = LIVE.keys.associateWith { camera ->
        val prefix = "rec_" + camera.removePrefix("cam_") + "_"
        RECORDINGS.keys.filter { it.startsWith(prefix) }
    }
}
