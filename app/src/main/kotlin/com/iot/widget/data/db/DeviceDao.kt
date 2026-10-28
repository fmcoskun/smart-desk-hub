package com.iot.widget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Data layer · SQL for the device manager (the same `devices` table DeskDao uses for the dashboard). */
@Dao
abstract class DeviceDao {

    @Query("SELECT * FROM devices ORDER BY sortOrder")
    abstract fun observeAll(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY sortOrder")
    abstract suspend fun getAll(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id")
    abstract suspend fun getById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(devices: List<DeviceEntity>)

    /** Rows from before version 3 have empty info columns: fill them in once, keep everything else. */
    @Query(
        "UPDATE devices SET firmware = :firmware, ipAddress = :ip, macAddress = :mac, batteryPct = :battery, " +
            "signalDbm = :signal, lastSeenMillis = :lastSeen WHERE id = :id AND firmware = ''",
    )
    abstract suspend fun backfillInfo(id: String, firmware: String, ip: String, mac: String, battery: Int?, signal: Int, lastSeen: Long)

    @Query("UPDATE devices SET name = :name WHERE id = :id")
    abstract suspend fun rename(id: String, name: String)

    @Query("UPDATE devices SET room = :room WHERE id = :id")
    abstract suspend fun setRoom(id: String, room: String)

    @Query("UPDATE devices SET online = :online, lastSeenMillis = :atMillis WHERE id = :id")
    abstract suspend fun setOnline(id: String, online: Boolean, atMillis: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM devices")
    abstract suspend fun maxSortOrder(): Int

    @Insert
    abstract suspend fun insert(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id = :id")
    abstract suspend fun delete(id: String)

    /**
     * Built-in devices cannot be deleted, so "insert if absent" never brings back something the
     * user removed; existing rows (renamed, moved) are left as they are.
     */
    @Transaction
    open suspend fun seed(devices: List<DeviceEntity>) {
        insertIfAbsent(devices)
        devices.forEach {
            backfillInfo(it.id, it.firmware, it.ipAddress, it.macAddress, it.batteryPct, it.signalDbm, it.lastSeenMillis)
        }
    }

    /** A new device goes to the end of the list. */
    @Transaction
    open suspend fun insertAtEnd(device: DeviceEntity) = insert(device.copy(sortOrder = maxSortOrder() + 1))
}
