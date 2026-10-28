package com.iot.widget.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iot.widget.domain.AudioOutput
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.Scene
import com.iot.widget.domain.ScheduleDays
import java.time.LocalTime

/**
 * Data layer · the `desk_settings` table. It always holds exactly one row (id = 0): the
 * settings of the dashboard and the detail screens. Room stores enums by name and LocalTime
 * through [Converters].
 */
@Entity(tableName = "desk_settings")
data class DeskSettingsEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val brightness: Int,
    val colorTempK: Int,
    val autoDim: Boolean,
    val scheduleOn: LocalTime,
    val scheduleOnDays: ScheduleDays,
    val scheduleOff: LocalTime,
    val scheduleOffDays: ScheduleDays,
    val scheduleEnabled: Boolean,
    val audioOutput: AudioOutput,
    val volume: Int,
    val plugTimer: PlugTimer,
    val plugTimerEndsAtMillis: Long?,
    val privacyOnLeave: Boolean,
    val privacyOnLock: Boolean,
    val activeScene: Scene,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}
