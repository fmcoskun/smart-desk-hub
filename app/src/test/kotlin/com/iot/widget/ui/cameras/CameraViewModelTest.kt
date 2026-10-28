package com.iot.widget.ui.cameras

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.DeviceId
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

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val desk = FakeDeskRepository()
    private val devices = FakeDeviceRepository()
    private val log = RecordingEventLog()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(id: String, user: String = "ayse"): CameraViewModel {
        val vm = CameraViewModel(SavedStateHandle(mapOf("id" to id)), devices, desk, FakeSessionRepository(startUserId = user), log, fixedClock())
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun snapshotLogsWithCameraDevice() = runTest(dispatcher) {
        val vm = viewModel("cam_door")
        vm.snapshot()
        runCurrent()
        assertEquals(listOf("Ayşe Yılmaz: took a snapshot [cam_door]"), log.lines)
        assertEquals(1, vm.uiState.value.snapshots)
    }

    @Test
    fun snapshotIgnoredWhenOfflineOrPrivacy() = runTest(dispatcher) {
        devices.setOnline("cam_hallway", online = false, atMillis = 0)
        val offline = viewModel("cam_hallway")
        offline.snapshot()
        desk.setDeviceOn(DeviceId.PrivacyGuard, true)
        val dark = viewModel("cam_desk")
        dark.snapshot()
        runCurrent()
        assertTrue(log.lines.isEmpty())
        assertEquals(0, dark.uiState.value.snapshots)
    }

    @Test
    fun fullScreenGoesDarkWhenPrivacyTurnsOn() = runTest(dispatcher) {
        val vm = viewModel("cam_desk")
        assertEquals(FeedState.LIVE, vm.uiState.value.tile?.feed)
        desk.setDeviceOn(DeviceId.PrivacyGuard, true)
        runCurrent()
        assertEquals(FeedState.PRIVACY, vm.uiState.value.tile?.feed)
    }

    @Test
    fun recordingsShortcutHiddenForGuest() = runTest(dispatcher) {
        assertTrue(viewModel("cam_door").uiState.value.canViewRecordings)
        assertFalse(viewModel("cam_door", user = "guest").uiState.value.canViewRecordings)
    }


    @Test
    fun snapshotTimerRestartsOnSecondTap() = runTest(dispatcher) {
        // each snapshot is a new event (the confirmation shows again), not a flag that stays true
        val vm = viewModel("cam_door")
        vm.snapshot()
        runCurrent()
        vm.snapshot()
        runCurrent()
        assertEquals(2, vm.uiState.value.snapshots)
    }
}
