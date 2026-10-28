package com.iot.widget.ui.cameras

import com.iot.widget.domain.DeviceId
import com.iot.widget.simulation.DeviceSimulator
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingEventLog
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class CamerasViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val desk = FakeDeskRepository()
    private val devices = FakeDeviceRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** A clock that moves with the test's virtual time (starts 14:21:34 UTC = 17:21:34 Istanbul). */
    private fun TestScope.virtualClock(): Clock = object : Clock() {
        private val start = fixedClock().millis()
        override fun getZone(): ZoneId = ZoneId.of("Europe/Istanbul")
        override fun withZone(zone: ZoneId?) = this
        override fun instant(): Instant = Instant.ofEpochMilli(start + testScheduler.currentTime)
    }

    private fun TestScope.viewModel(user: String = "ayse", clock: Clock = fixedClock()): CamerasViewModel {
        val vm = CamerasViewModel(devices, desk, FakeSessionRepository(startUserId = user), clock)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun fourTilesInSeedOrderWithRepositoryNames() = runTest(dispatcher) {
        devices.rename("cam_door", "Front Door")
        val vm = viewModel()
        val tiles = vm.uiState.value.tiles
        assertEquals(listOf("cam_desk", "cam_door", "cam_meeting", "cam_hallway"), tiles.map { it.id })
        assertEquals("Front Door", tiles[1].name)
        assertEquals("live_door", tiles[1].clip)
        assertTrue(tiles.all { it.feed == FeedState.LIVE })
        assertTrue(vm.uiState.value.canViewRecordings)
    }

    @Test
    fun privacyGuardDarkensOnlyDeskCamera() = runTest(dispatcher) {
        val vm = viewModel()
        desk.setDeviceOn(DeviceId.PrivacyGuard, true)
        runCurrent()
        assertEquals(listOf(FeedState.PRIVACY, FeedState.LIVE, FeedState.LIVE, FeedState.LIVE), vm.uiState.value.tiles.map { it.feed })
        desk.setDeviceOn(DeviceId.PrivacyGuard, false)
        runCurrent()
        assertEquals(FeedState.LIVE, vm.uiState.value.tiles.first().feed)
    }

    @Test
    fun restartingCameraIsOffline() = runTest(dispatcher) {
        val sim = DeviceSimulator(devices, RecordingEventLog(), fixedClock(), Random(1), backgroundScope)
        val vm = viewModel()
        sim.restart("cam_hallway")
        runCurrent()
        assertEquals(FeedState.OFFLINE, vm.uiState.value.tiles.last().feed)
        advanceTimeBy(DeviceSimulator.RESTART_MS + 1)
        runCurrent()
        assertEquals(FeedState.LIVE, vm.uiState.value.tiles.last().feed)
    }

    @Test
    fun clockTicksEverySecond() = runTest(dispatcher) {
        val vm = viewModel(clock = virtualClock())
        assertEquals("17:21:34", vm.uiState.value.clock)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("17:21:35", vm.uiState.value.clock)
    }

    @Test
    fun guestSeesCamerasButNotRecordings() = runTest(dispatcher) {
        val vm = viewModel(user = "guest")
        assertEquals(4, vm.uiState.value.tiles.size)
        assertFalse(vm.uiState.value.canViewRecordings)
    }
}
