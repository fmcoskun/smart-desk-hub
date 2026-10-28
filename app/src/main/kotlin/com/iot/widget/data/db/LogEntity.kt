package com.iot.widget.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogSource

/**
 * Data layer · one row of the `logs` table (database version 4). The index on `timeMillis`
 * keeps "newest first within a date range" fast even with thousands of rows.
 */
@Entity(tableName = "logs", indices = [Index("timeMillis")])
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val source: LogSource,
    val level: LogLevel,
    val actor: String?,
    val deviceId: String?,
    val deviceName: String?,
    val message: String,
    val topic: String?,
    val payload: String?,
)
