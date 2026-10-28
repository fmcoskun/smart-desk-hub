package com.iot.widget.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iot.widget.domain.cameras.RecordingEvent

/** Data layer · one row of the `recordings` table (database version 5). */
@Entity(tableName = "recordings", indices = [Index("startMillis")])
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cameraId: String,
    val startMillis: Long,
    val durationMs: Long,
    val event: RecordingEvent,
    val clip: String,
)
