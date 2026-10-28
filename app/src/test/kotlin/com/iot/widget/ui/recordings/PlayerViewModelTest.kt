package com.iot.widget.ui.recordings

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.cameras.Recording
import com.iot.widget.domain.cameras.RecordingEvent
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = fixedClock()
    private val rec = Recording(1, "cam_door", clock.millis() - 3_600_000, 45_000, RecordingEvent.PERSON, "rec_45")

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(user: String = "ayse"): PlayerViewModel {
        val vm = PlayerViewModel(SavedStateHandle(mapOf("id" to 1L)), FakeRecordingRepository(listOf(rec), clock), FakeDeviceRepository(), FakeSessionRepository(startUserId = user), clock)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun loadsRecordingInfo() = runTest(dispatcher) {
        val state = viewModel().uiState.value
        assertTrue(state.allowed)
        assertEquals(PlayerInfoUi("rec_45", "Door Camera", "Tue 27 Oct, 16:21:34", 45_000, RecordingEvent.PERSON), state.info)
    }

    @Test
    fun guestCannotOpenPlayer() = runTest(dispatcher) {
        val state = viewModel(user = "guest").uiState.value
        assertFalse(state.allowed)
        assertTrue(state.loaded)
        assertNull(state.info)
    }


    @Test
    fun unknownRecordingShowsNotFound() = runTest(dispatcher) {
        val vm = PlayerViewModel(SavedStateHandle(mapOf("id" to 99L)), FakeRecordingRepository(listOf(rec), clock), FakeDeviceRepository(), FakeSessionRepository(startUserId = "ayse"), clock)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        assertTrue(vm.uiState.value.notFound)
        assertNull(vm.uiState.value.info)
    }
}
