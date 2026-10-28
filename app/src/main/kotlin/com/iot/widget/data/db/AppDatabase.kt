package com.iot.widget.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Data layer · the SQLite database file "smartdesk.db". Listing the entities here creates the
 * tables. Each schema change bumps `version`; the exported schemas in app/schemas/ document
 * every version, and an AutoMigration lets Room generate the upgrade SQL from them:
 *  - v1 (step 1): devices, desk_settings
 *  - v2 (step 2): + users — a phone upgrading from step 1 keeps all its desk data
 *  - v3 (step 3): + device info columns (firmware, IP, MAC, battery, signal, last seen)
 *  - v4 (step 4): + logs (the searchable event log)
 *  - v5 (step 5): + recordings (camera clips with their event tag)
 */
@Database(
    entities = [DeviceEntity::class, DeskSettingsEntity::class, UserEntity::class, LogEntity::class, RecordingEntity::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deskDao(): DeskDao
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun logDao(): LogDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        const val NAME = "smartdesk.db"
    }
}
