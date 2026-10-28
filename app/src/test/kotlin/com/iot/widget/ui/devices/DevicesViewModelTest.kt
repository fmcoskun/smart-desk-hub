package com.iot.widget.ui.devices

import com.iot.widget.domain.devices.Room
import com.iot.widget.simulation.DeviceSimulator
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
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(user: String = "ayse"): DevicesViewModel {
        val repo = FakeDeviceRepository()
        val sim = DeviceSimulator(repo, RecordingEventLog(), fixedClock(), Random(1), backgroundScope)
        val vm = DevicesViewModel(repo, FakeSessionRepository(startUserId = user), sim)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun filterByRoomAndOffline() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(10, vm.uiState.value.rows.size)
        vm.setFilter(DeviceFilter.MEETING_ROOM)
        runCurrent()
        assertEquals(listOf("cam_meeting", "meeting_sensor"), vm.uiState.value.rows.map { it.id })
        vm.setFilter(DeviceFilter.OFFLINE)
        runCurrent()
        assertEquals(listOf("meeting_sensor"), vm.uiState.value.rows.map { it.id })
        assertEquals(Room.MEETING_ROOM, vm.uiState.value.rows.single().room)
    }

    @Test
    fun countsPerFilter() = runTest(dispatcher) {
        val counts = viewModel().uiState.value.counts
        assertEquals(10, counts[DeviceFilter.ALL])
        assertEquals(6, counts[DeviceFilter.DESK])
        assertEquals(1, counts[DeviceFilter.OFFLINE])
        assertEquals(1, counts[DeviceFilter.ENTRANCE])
    }

    @Test
    fun addOnlyForAdmin() = runTest(dispatcher) {
        assertTrue(viewModel("ayse").uiState.value.canAdd)
        assertFalse(viewModel("mehmet").uiState.value.canAdd)
        assertFalse(viewModel("guest").uiState.value.canAdd)
    }

    @Test
    fun batteryAndFirmwareSummaries() = runTest(dispatcher) {
        val rows = viewModel().uiState.value.rows
        assertEquals(DeviceSummary.Battery(12), rows.first { it.id == "meeting_sensor" }.summary)
        assertEquals(DeviceSummary.Firmware("1.0.0"), rows.first { it.id == "cam_desk" }.summary)
    }
}
