package com.iot.widget.data

import com.iot.widget.domain.withDevice
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.iot.widget.data.db.AppDatabase
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.isOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Repository against a real (in-memory or file) Room database on Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomDeskRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val openDbs = mutableListOf<AppDatabase>()

    private fun inMemory(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
            .also { openDbs += it }

    @After
    fun close() = openDbs.forEach { it.close() }

    @Test
    fun seedCreatesFourDevicesAndDefaults() = runTest {
        val repo = RoomDeskRepository(inMemory().deskDao())
        repo.ensureSeeded()
        val state = repo.current()
        assertEquals(4, state.devices.size)
        assertEquals("Desk Halo LED", state.devices.getValue(DeviceId.DeskLight).name)
        assertEquals(80, state.settings.brightness)
        assertFalse(state.isOn(DeviceId.PrivacyGuard))
        assertTrue(state.isOn(DeviceId.SmartPlug))
    }

    @Test
    fun ensureSeededIsIdempotent() = runTest {
        val repo = RoomDeskRepository(inMemory().deskDao())
        repo.ensureSeeded()
        repo.updateSettings { it.copy(brightness = 50) }
        repo.setDeviceOn(DeviceId.Soundbar, false)
        repo.ensureSeeded()
        assertEquals(50, repo.current().settings.brightness)
        assertFalse(repo.current().isOn(DeviceId.Soundbar))
    }

    @Test
    fun stateSurvivesDatabaseReopen() = runTest {
        val name = "reopen-test.db"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        RoomDeskRepository(first.deskDao()).apply {
            ensureSeeded()
            updateSettings { it.copy(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 1_800_000L) }
            setDeviceOn(DeviceId.Soundbar, false)
        }
        first.close()

        val second = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        openDbs += second
        val state = RoomDeskRepository(second.deskDao()).current()
        assertFalse(state.isOn(DeviceId.Soundbar))
        assertEquals(PlugTimer.M30, state.settings.plugTimer)
        assertEquals(1_800_000L, state.settings.plugTimerEndsAtMillis)
    }

    @Test
    fun turningPlugOffClearsTimer() = runTest {
        val repo = RoomDeskRepository(inMemory().deskDao())
        repo.ensureSeeded()
        repo.updateSettings { it.copy(plugTimer = PlugTimer.H1, plugTimerEndsAtMillis = 99L) }
        repo.setDeviceOn(DeviceId.SmartPlug, false)
        assertEquals(PlugTimer.Off, repo.current().settings.plugTimer)
    }

    @Test
    fun stateFlowEmitsOnChange() = runTest {
        val repo = RoomDeskRepository(inMemory().deskDao())
        repo.ensureSeeded()
        repo.state.test {
            assertTrue(awaitItem().isOn(DeviceId.Soundbar))
            repo.setDeviceOn(DeviceId.Soundbar, false)
            assertFalse(awaitItem().isOn(DeviceId.Soundbar))
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun timerCleanupOnlyIfStillPlanned() = runTest {
        val repo = RoomDeskRepository(inMemory().deskDao())
        repo.ensureSeeded()
        repo.updateSettings { it.copy(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 1_000L) }
        // the user re-set the timer since this job was planned: nothing happens
        assertFalse(repo.turnOffPlugIfPlanned(999L))
        assertTrue(repo.current().isOn(DeviceId.SmartPlug))
        assertEquals(PlugTimer.M30, repo.current().settings.plugTimer)
        // the planned end matches: plug off and timer cleared, in one step
        assertTrue(repo.turnOffPlugIfPlanned(1_000L))
        assertFalse(repo.current().isOn(DeviceId.SmartPlug))
        assertEquals(PlugTimer.Off, repo.current().settings.plugTimer)
    }

    @Test
    fun updateStateChangesOnlySwitchesAndSettings() = runTest {
        val repo = RoomDeskRepository(inMemory().deskDao())
        repo.ensureSeeded()
        val (before, after) = repo.updateState { s ->
            // a stale device name in the snapshot must not be written back
            val renamed = s.devices.mapValues { (_, d) -> d.copy(name = "stale") }
            s.copy(devices = renamed).withDevice(DeviceId.Soundbar, false).let { it.copy(settings = it.settings.copy(brightness = 40)) }
        }
        assertTrue(before.isOn(DeviceId.Soundbar))
        assertFalse(after.isOn(DeviceId.Soundbar))
        val now = repo.current()
        assertFalse(now.isOn(DeviceId.Soundbar))
        assertEquals(40, now.settings.brightness)
        assertEquals("Bluetooth", now.devices.getValue(DeviceId.Soundbar).name)
    }

    @Test
    fun deviceManagerChangesReachTheDashboardState() = runTest {
        // one real database: the device manager and the dashboard read the same `devices` rows
        val db = inMemory()
        val desk = RoomDeskRepository(db.deskDao())
        desk.ensureSeeded()
        val devices = RoomDeviceRepository(db.deviceDao(), com.iot.widget.testutil.fixedClock())
        devices.rename("desk_light", "Lamp X")
        devices.setOnline("smart_plug", online = false, atMillis = 0)
        val state = desk.state.first()
        assertEquals("Lamp X", state.devices.getValue(DeviceId.DeskLight).name)
        assertFalse(state.devices.getValue(DeviceId.SmartPlug).online)
    }
}
