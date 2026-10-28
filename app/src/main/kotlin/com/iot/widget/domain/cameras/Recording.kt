package com.iot.widget.domain.cameras

/** What the camera's motion detection tagged the clip with (spec §6.7). */
enum class RecordingEvent { PERSON, MOTION }

/**
 * Domain layer · one stored camera recording. [clip] is the raw video it plays (see [Clips]);
 * [durationMs] is that clip's real length, so the list never promises more than it plays.
 */
data class Recording(
    val id: Long,
    val cameraId: String,
    val startMillis: Long,
    val durationMs: Long,
    val event: RecordingEvent,
    val clip: String,
)
