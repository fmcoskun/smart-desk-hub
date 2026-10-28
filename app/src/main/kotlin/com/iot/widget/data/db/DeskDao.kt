package com.iot.widget.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.iot.widget.domain.PlugTimer
import kotlinx.coroutines.flow.Flow

/**
 * Data layer · DAO (Data Access Object): the SQL for the desk tables. Room generates the
 * implementation at compile time (via KSP) and checks every query against the schema.
 *
 * - Functions returning Flow are *live*: they emit again whenever the table changes, which is
 *   how the dashboard redraws itself after a background worker turns the plug off.
 * - `suspend` functions run off the main thread, so the UI never freezes on disk access.
 * - @Transaction groups several statements: either all happen or none.
 */
@Dao
abstract class DeskDao {

    @Query("SELECT * FROM devices ORDER BY sortOrder")
    abstract fun observeDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM desk_settings WHERE id = 0")
    abstract fun observeSettings(): Flow<DeskSettingsEntity?>

    @Query("SELECT * FROM devices ORDER BY sortOrder")
    abstract suspend fun getDevices(): List<DeviceEntity>

    @Query("SELECT * FROM desk_settings WHERE id = 0")
    abstract suspend fun getSettings(): DeskSettingsEntity?

    /** IGNORE = rows that already exist are left untouched (keeps the user's changes on re-seed). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertDevicesIfAbsent(devices: List<DeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertSettingsIfAbsent(settings: DeskSettingsEntity)

    @Upsert
    abstract suspend fun upsertDevices(devices: List<DeviceEntity>)

    @Upsert
    abstract suspend fun upsertSettings(settings: DeskSettingsEntity)

    @Query("UPDATE devices SET isOn = :on WHERE id = :id")
    abstract suspend fun setDeviceOn(id: String, on: Boolean)

    @Query("UPDATE desk_settings SET plugTimer = :timer, plugTimerEndsAtMillis = NULL WHERE id = 0")
    abstract suspend fun clearPlugTimer(timer: PlugTimer = PlugTimer.Off)

    /** Turning the plug off also cancels its auto-off timer, in the same transaction. */
    @Transaction
    open suspend fun setDeviceOnAndClearTimer(id: String, on: Boolean, isPlug: Boolean) {
        setDeviceOn(id, on)
        if (isPlug && !on) clearPlugTimer()
    }

    /** Read-modify-write of the settings row inside one transaction. */
    @Transaction
    open suspend fun updateSettings(transform: (DeskSettingsEntity) -> DeskSettingsEntity) {
        val current = getSettings() ?: return
        upsertSettings(transform(current))
    }

    /**
     * Read-change-write of the whole desk in one transaction. [change] gets the current rows and
     * returns the new on/off per device id plus the new settings; only those are written.
     */
    @Transaction
    open suspend fun updateDesk(change: (List<DeviceEntity>, DeskSettingsEntity) -> Pair<Map<String, Boolean>, DeskSettingsEntity>) {
        val settings = getSettings() ?: return
        val (switches, newSettings) = change(getDevices(), settings)
        switches.forEach { (id, on) -> setDeviceOn(id, on) }
        upsertSettings(newSettings)
    }

    /** "Clear the timer if it still ends at :planned" — the WHERE makes check and change one step. */
    @Query("UPDATE desk_settings SET plugTimer = :timer, plugTimerEndsAtMillis = NULL WHERE id = 0 AND plugTimerEndsAtMillis = :planned")
    abstract suspend fun clearPlugTimerIfPlanned(planned: Long, timer: PlugTimer = PlugTimer.Off): Int

    @Transaction
    open suspend fun turnOffPlugIfPlanned(plugId: String, planned: Long): Boolean {
        val wasOn = getDevices().firstOrNull { it.id == plugId }?.isOn == true
        if (clearPlugTimerIfPlanned(planned) == 0) return false
        setDeviceOn(plugId, false)
        return wasOn
    }
}
