package com.iot.widget.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data layer · one row of the `devices` table. @Entity tells Room to create a table from this
 * class; each property becomes a column.
 *
 * Entities are kept separate from the domain models (DeviceState for the dashboard, Device for
 * the device manager): the database shape can change without touching screens.
 *
 * The info columns were added in database version 3. `defaultValue` tells the automatic
 * migration what to put into rows that already existed on an upgraded phone.
 */
@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subtitle: String,
    /** DeviceType name; step-1 rows hold lowercase names ("soundbar"), read case-insensitively. */
    val type: String,
    /** Room name; step-1 rows hold "Desk", read case-insensitively. */
    val room: String,
    val isOn: Boolean,
    val online: Boolean,
    val sortOrder: Int,
    @ColumnInfo(defaultValue = "") val firmware: String = "",
    @ColumnInfo(defaultValue = "") val ipAddress: String = "",
    @ColumnInfo(defaultValue = "") val macAddress: String = "",
    val batteryPct: Int? = null,
    @ColumnInfo(defaultValue = "-50") val signalDbm: Int = -50,
    @ColumnInfo(defaultValue = "0") val lastSeenMillis: Long = 0,
)
