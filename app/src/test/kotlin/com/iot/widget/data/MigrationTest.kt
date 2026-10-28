package com.iot.widget.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.isOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Database upgrades: v1 → v2 (step 2 adds `users`), v2 → v3 (step 3 adds device info
 * columns), v3 → v4 (step 4 adds `logs`) and v4 → v5 (step 5 adds `recordings`). Existing data must survive. Each old database is built from its exported schema
 * (app/schemas/.../N.json), exactly as a phone running that older app version would have it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migrate1To2KeepsDeskData() = runTest {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.iot.widget.data.db.AppDatabase/1.json").readText()).getJSONObject("database")

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { v1 ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                v1.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v1.execSQL(setup.getString(i))
            v1.version = 1
            RoomDeskRepository.SEED_DEVICES.forEach { d ->
                v1.execSQL(
                    "INSERT INTO devices VALUES (?,?,?,?,?,?,?,?)",
                    arrayOf<Any>(d.id, d.name, d.subtitle, d.type, d.room, if (d.id == "soundbar") 0 else 1, 1, d.sortOrder),
                )
            }
            v1.execSQL(
                "INSERT INTO desk_settings VALUES (0, 42, 4000, 1, 510, 'WEEKDAYS', 1140, 'EVERY_DAY', 1, 'Soundbar', 45, 'Off', NULL, 1, 0, 'Work')",
            )
        }

        val v2 = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        val desk = RoomDeskRepository(v2.deskDao()).current()
        assertFalse(desk.isOn(DeviceId.Soundbar))
        assertEquals(42, desk.settings.brightness)
        assertEquals(4, RoomUserRepository(v2.userDao()).all().size) // users table created and seeded
        v2.close()
    }

    @Test
    fun migrate2To3KeepsDataAndAddsDevices() = runTest {
        val name = "migration-test-2.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.iot.widget.data.db.AppDatabase/2.json").readText()).getJSONObject("database")

        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { v2 ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                v2.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v2.execSQL(setup.getString(i))
            v2.version = 2
            // step-2 data: the soundbar was renamed, Can was added as a user
            RoomDeskRepository.SEED_DEVICES.forEach { d ->
                val label = if (d.id == "soundbar") "Office Speaker" else d.name
                v2.execSQL("INSERT INTO devices VALUES (?,?,?,?,?,?,?,?)", arrayOf<Any>(d.id, label, d.subtitle, d.type, d.room, 1, 1, d.sortOrder))
            }
            v2.execSQL("INSERT INTO desk_settings VALUES (0, 42, 4000, 1, 510, 'WEEKDAYS', 1140, 'EVERY_DAY', 1, 'Soundbar', 45, 'Off', NULL, 1, 0, 'Work')")
            v2.execSQL("INSERT INTO users VALUES ('can', 'Can Öztürk', 'OPERATOR', 1, NULL, 0)")
        }

        val v3 = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        val devices = RoomDeviceRepository(v3.deviceDao(), com.iot.widget.testutil.fixedClock()).devices.first()
        assertEquals("Office Speaker", devices.first { it.id == "soundbar" }.name)
        assertEquals(10, devices.size)
        assertEquals("2.4.1", devices.first { it.id == "soundbar" }.info.firmware) // backfilled
        assertEquals("Can Öztürk", RoomUserRepository(v3.userDao()).byId("can")!!.name)
        assertEquals(42, RoomDeskRepository(v3.deskDao()).current().settings.brightness)
        v3.close()
    }

    @Test
    fun migrate3To4KeepsDataAndAddsLogs() = runTest {
        val name = "migration-test-3.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.iot.widget.data.db.AppDatabase/3.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { v3 ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                v3.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v3.execSQL(setup.getString(i))
            v3.version = 3
            v3.execSQL("INSERT INTO users VALUES ('can', 'Can Öztürk', 'OPERATOR', 1, NULL, 0)")
            v3.execSQL("INSERT INTO devices (id, name, subtitle, type, room, isOn, online, sortOrder) VALUES ('cam_desk', 'Webcam', '', 'CAMERA', 'DESK', 1, 1, 10)")
        }
        val v4 = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        assertEquals("Can Öztürk", RoomUserRepository(v4.userDao()).byId("can")!!.name)
        assertEquals("Webcam", RoomDeviceRepository(v4.deviceDao(), com.iot.widget.testutil.fixedClock()).byId("cam_desk")!!.name)
        assertEquals(0, RoomLogRepository(v4.logDao(), com.iot.widget.testutil.fixedClock()).observe(com.iot.widget.domain.logs.LogQuery()).first().total)
        v4.close()
    }

    @Test
    fun migrate4To5KeepsLogsAndAddsRecordings() = runTest {
        val name = "migration-test-4.db"
        context.deleteDatabase(name)
        val schema = JSONObject(File("schemas/com.iot.widget.data.db.AppDatabase/4.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { v4 ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val e = entities.getJSONObject(i)
                v4.execSQL(e.getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                val indices = e.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    v4.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", e.getString("tableName")))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) v4.execSQL(setup.getString(i))
            v4.version = 4
            v4.execSQL("INSERT INTO logs (timeMillis, source, level, actor, message) VALUES (1, 'CLIENT', 'INFO', 'Can', 'signed in')")
        }
        val v5 = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        val logs = RoomLogRepository(v5.logDao(), com.iot.widget.testutil.fixedClock())
        assertEquals("signed in", logs.byId(1)!!.message)
        val recordings = RoomRecordingRepository(v5.recordingDao(), com.iot.widget.testutil.fixedClock())
        assertEquals(0, recordings.observe(null, com.iot.widget.domain.logs.DateRange.Last7Days).first().size)
        v5.close()
    }
}
