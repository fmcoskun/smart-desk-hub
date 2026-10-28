package com.iot.widget.simulation

import com.iot.widget.domain.devices.BuiltIn
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.RecordingEventLog
import com.iot.widget.testutil.builtInTestDevices
import com.iot.widget.testutil.fixedClock
import com.iot.widget.testutil.testDevice
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceSimulatorTest {

    private val log = RecordingEventLog()

    private fun TestScope.simulator(repo: FakeDeviceRepository, random: Random = Random(7)) =
        DeviceSimulator(repo, log, fixedClock(), random, backgroundScope)

    @Test
    fun restartGoesOfflineThenOnlineAfter5s() = runTest {
        val repo = FakeDeviceRepository()
        val sim = simulator(repo)
        assertTrue(sim.restart("smart_plug"))
        runCurrent()
        assertFalse(repo.flow.value.first { it.id == "smart_plug" }.online)
        assertEquals(setOf("smart_plug"), sim.restarting.value)

        advanceTimeBy(DeviceSimulator.RESTART_MS + 1)
        runCurrent()
        assertTrue(repo.flow.value.first { it.id == "smart_plug" }.online)
        assertTrue(sim.restarting.value.isEmpty())
        assertEquals(listOf("WARN smart_plug: restarting…", "INFO smart_plug: back online"), log.deviceLines)
    }

    @Test
    fun restartWhileRestartingIsIgnored() = runTest {
        val sim = simulator(FakeDeviceRepository())
        assertTrue(sim.restart("cam_desk"))
        runCurrent()
        assertFalse(sim.restart("cam_desk"))
        advanceTimeBy(DeviceSimulator.RESTART_MS + 1)
        runCurrent()
        assertEquals(2, log.deviceLines.size) // one cycle only
    }

    @Test
    fun eventsAtMostOncePerMinuteAndNeverFromDashboardDevices() = runTest {
        val sim = simulator(FakeDeviceRepository())
        sim.start()
        sim.start() // idempotent
        advanceTimeBy(10 * 60_000L + 1)
        runCurrent()
        assertTrue(log.deviceLines.size in 1..10)
        assertTrue(log.deviceLines.none { line -> BuiltIn.DASHBOARD_IDS.any { line.contains(" $it:") } })
    }

    @Test
    fun startBringsBackDevicesLeftOfflineByAnInterruptedRestart() = runTest {
        // the app was killed during a restart: Smart Plug stayed offline in the database
        val devices = builtInTestDevices().map { if (it.id == "smart_plug") it.copy(online = false) else it }
        val repo = FakeDeviceRepository(devices)
        simulator(repo).start()
        runCurrent()
        assertTrue(repo.flow.value.first { it.id == "smart_plug" }.online)
        // the sensor that is offline by design stays offline
        assertFalse(repo.flow.value.first { it.id == BuiltIn.MEETING_SENSOR }.online)
    }

    @Test
    fun lowBatteryOnlyForLowBattery() = runTest {
        val devices = builtInTestDevices().filter { it.id in BuiltIn.DASHBOARD_IDS } +
            testDevice("weak", battery = 15) + testDevice("strong", battery = 90)
        val sim = simulator(FakeDeviceRepository(devices), Random(3))
        sim.start()
        advanceTimeBy(60 * 60_000L)
        runCurrent()
        assertTrue(log.deviceLines.isNotEmpty())
        assertTrue(log.deviceLines.filter { it.contains("low battery") }.all { it.contains(" weak:") })
        assertTrue(log.deviceLines.none { it.startsWith("ERROR") }) // nobody is offline
    }


    @Test
    fun restartOfDeletedDeviceLogsNoBackOnline() = runTest {
        val repo = FakeDeviceRepository(com.iot.widget.testutil.builtInTestDevices() + com.iot.widget.testutil.testDevice("lamp2", "Lamp 2"))
        val sim = simulator(repo)
        sim.restart("lamp2")
        runCurrent()
        repo.delete("lamp2")
        advanceTimeBy(DeviceSimulator.RESTART_MS + 1)
        runCurrent()
        assertEquals(listOf("WARN lamp2: restarting…"), log.deviceLines)
    }
}
