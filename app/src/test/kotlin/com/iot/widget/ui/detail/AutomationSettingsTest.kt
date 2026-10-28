package com.iot.widget.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.ScheduleDays
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.simulation.SensorSimulator
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeExactAlarms
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeScheduler
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.defaultDeskState
import com.iot.widget.testutil.fixedClock
import com.iot.widget.ui.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime
import kotlin.random.Random

/** Detail settings that drive background automations (spec §9b). */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AutomationSettingsTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = fixedClock()
    private val publisher = RecordingPublisher()
    private val scheduler = FakeScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    // runCurrent(): let the ViewModel load the signed-in user's permissions before it is used
    private fun TestScope.detail(repo: FakeDeskRepository, type: DetailType) =
        DetailViewModel(SavedStateHandle(mapOf("type" to type.key)), repo, publisher, scheduler, FakeExactAlarms(allowed = true), FakeSessionRepository(), clock).also { vm -> backgroundScope.launch(dispatcher) { vm.uiState.collect {} }; runCurrent() }

    @Test
    fun selectingTimerSchedulesAndPublishes() = runTest(dispatcher) {
        val repo = FakeDeskRepository()
        val vm = detail(repo, DetailType.Rig)
        runCurrent()
        publisher.messages.clear()

        vm.setPlugTimer(PlugTimer.M30)
        runCurrent()

        assertEquals(listOf("start:30"), scheduler.calls)
        assertEquals(PlugTimer.M30, repo.flow.value.settings.plugTimer)
        assertEquals(clock.millis() + 30 * 60_000L, repo.flow.value.settings.plugTimerEndsAtMillis)
        assertEquals(Topics.PLUG_TIMER, publisher.messages.last().topic)
        assertEquals("""{"value":"30"}""", publisher.messages.last().json)
    }

    @Test
    fun selectingTimerTurnsPlugOnWhenOff() = runTest(dispatcher) {
        val start = defaultDeskState().let {
            it.copy(devices = it.devices + (DeviceId.SmartPlug to it.devices.getValue(DeviceId.SmartPlug).copy(isOn = false)))
        }
        val repo = FakeDeskRepository(start)
        detail(repo, DetailType.Rig).setPlugTimer(PlugTimer.H1)
        runCurrent()
        assertTrue(repo.flow.value.isOn(DeviceId.SmartPlug))
        assertEquals(PlugTimer.H1, repo.flow.value.settings.plugTimer)
    }

    @Test
    fun plugTimerOnOfflinePlugIgnored() = runTest(dispatcher) {
        val start = defaultDeskState().let {
            it.copy(devices = it.devices + (DeviceId.SmartPlug to it.devices.getValue(DeviceId.SmartPlug).copy(isOn = false, online = false)))
        }
        val repo = FakeDeskRepository(start)
        val vm = detail(repo, DetailType.Rig)
        assertFalse(vm.uiState.value!!.deviceOnline)
        publisher.messages.clear()
        vm.setPlugTimer(PlugTimer.M30)
        runCurrent()
        assertFalse(repo.flow.value.isOn(DeviceId.SmartPlug))
        assertTrue(publisher.messages.isEmpty())
        assertTrue(scheduler.calls.isEmpty())
    }

    @Test
    fun selectingOffCancels() = runTest(dispatcher) {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 5L)))
        detail(repo, DetailType.Rig).setPlugTimer(PlugTimer.Off)
        runCurrent()
        assertEquals(listOf("cancel"), scheduler.calls)
        assertEquals(PlugTimer.Off, repo.flow.value.settings.plugTimer)
        assertNull(repo.flow.value.settings.plugTimerEndsAtMillis)
        assertEquals("""{"value":"off"}""", publisher.messages.last().json)
    }

    @Test
    fun scheduleSaveReschedulesAndPublishes() = runTest(dispatcher) {
        val repo = FakeDeskRepository()
        detail(repo, DetailType.Light).setSchedule(LocalTime.of(8, 30), ScheduleDays.WEEKDAYS, LocalTime.of(19, 0), ScheduleDays.EVERY_DAY, enabled = true)
        runCurrent()
        assertEquals(listOf("light:Replace"), scheduler.calls)
        assertEquals(Topics.LIGHTS_SCHEDULE, publisher.messages.last().topic)
        assertEquals(
            """{"on":"08:30","onDays":"WEEKDAYS","off":"19:00","offDays":"EVERY_DAY","enabled":true}""",
            publisher.messages.last().json,
        )
    }

    @Test
    fun autoDimReschedulesSunset() = runTest(dispatcher) {
        val repo = FakeDeskRepository()
        detail(repo, DetailType.Light).setAutoDim(false)
        runCurrent()
        assertEquals(listOf("sunset:Replace"), scheduler.calls)
    }

    @Test
    fun manualPlugOffCancelsTimer() = runTest(dispatcher) {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(plugTimer = PlugTimer.M30, plugTimerEndsAtMillis = 5L)))
        val vm = DashboardViewModel(repo, publisher, SensorSimulator(Random(1)), scheduler, FakeSessionRepository(), FakeDeviceRepository(), clock, Random(2))
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        vm.onToggle(DeviceId.SmartPlug, false)
        runCurrent()
        assertEquals(listOf("cancel"), scheduler.calls)
        assertEquals(PlugTimer.Off, repo.flow.value.settings.plugTimer)
    }
}
