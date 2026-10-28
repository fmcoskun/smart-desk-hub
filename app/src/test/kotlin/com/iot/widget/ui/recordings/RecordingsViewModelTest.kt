package com.iot.widget.ui.recordings

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.cameras.Recording
import com.iot.widget.domain.cameras.RecordingEvent
import com.iot.widget.domain.logs.DateRange
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeRecordingRepository
import com.iot.widget.testutil.FakeSessionRepository
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
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class RecordingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = fixedClock() // Tue 2026-10-27 17:21:34 Istanbul
    private val zone = clock.zone

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun at(day: Int, hour: Int, minute: Int) =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private val sample = listOf(
        Recording(1, "cam_door", at(27, 14, 5), 45_000, RecordingEvent.PERSON, "rec_45"),
        Recording(2, "cam_desk", at(27, 9, 30), 15_000, RecordingEvent.MOTION, "rec_15"),
        Recording(3, "cam_door", at(26, 23, 59), 30_000, RecordingEvent.MOTION, "rec_30"),
        Recording(4, "cam_hallway", at(25, 8, 0), 60_000, RecordingEvent.PERSON, "rec_60"),
    )

    private fun TestScope.viewModel(user: String = "ayse", cameraId: String? = null): RecordingsViewModel {
        val handle = SavedStateHandle(if (cameraId == null) emptyMap() else mapOf("cameraId" to cameraId))
        val vm = RecordingsViewModel(handle, FakeRecordingRepository(sample, clock), FakeDeviceRepository(), FakeSessionRepository(startUserId = user), clock)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun todayNewestFirstWithNamesTimesAndDurations() = runTest(dispatcher) {
        val rows = viewModel().uiState.value.rows
        assertEquals(listOf(1L, 2L), rows.map { it.id })
        assertEquals(RecordingRowUi(1, "Door Camera", "14:05", "0:45", RecordingEvent.PERSON), rows.first())
    }

    @Test
    fun dayFilterUsesLocalDays() = runTest(dispatcher) {
        val vm = viewModel()
        vm.setRange(DateRange.Yesterday)
        runCurrent()
        assertEquals(listOf(3L), vm.uiState.value.rows.map { it.id }) // 23:59 yesterday is not "today"
        vm.setRange(DateRange.Custom(java.time.LocalDate.of(2026, 10, 25), java.time.LocalDate.of(2026, 10, 25)))
        runCurrent()
        assertEquals(listOf(4L), vm.uiState.value.rows.map { it.id })
    }

    @Test
    fun cameraFilterAndShortcutPrefilter() = runTest(dispatcher) {
        val vm = viewModel(cameraId = "cam_door")
        assertEquals("cam_door", vm.uiState.value.cameraId)
        assertEquals(listOf(1L), vm.uiState.value.rows.map { it.id })
        vm.setCamera(null)
        runCurrent()
        assertEquals(2, vm.uiState.value.rows.size)
        assertEquals(listOf("cam_desk", "cam_door", "cam_meeting", "cam_hallway"), vm.uiState.value.cameras.map { it.first })
    }

    @Test
    fun guestGetsNoRecordings() = runTest(dispatcher) {
        val vm = viewModel(user = "guest")
        assertFalse(vm.uiState.value.allowed)
        assertTrue(vm.uiState.value.loaded)
        assertTrue(vm.uiState.value.rows.isEmpty())
    }
}
