package com.iot.widget.ui.logs

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.logs.LogEntry
import com.iot.widget.domain.logs.LogLevel
import com.iot.widget.domain.logs.LogSource
import com.iot.widget.testutil.FakeLogRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = fixedClock()
    private val logs = FakeLogRepository(listOf(LogEntry(0, clock.millis(), LogSource.CLIENT, LogLevel.INFO, "Ayşe Yılmaz", null, null, "signed in", null, null)), clock)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(id: Long, user: String = "ayse"): LogDetailViewModel {
        val vm = LogDetailViewModel(SavedStateHandle(mapOf("id" to id)), logs, FakeSessionRepository(startUserId = user), clock)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun loadsTheEntry() = runTest(dispatcher) {
        val s = viewModel(1).state.value
        assertTrue(s is LogDetailState.Loaded)
        assertEquals("signed in", (s as LogDetailState.Loaded).ui.entry.message)
    }

    @Test
    fun logDetailGuestNotAllowed() = runTest(dispatcher) {
        assertEquals(LogDetailState.NotAllowed, viewModel(1, user = "guest").state.value)
    }

    @Test
    fun unknownLogShowsNotFound() = runTest(dispatcher) {
        // e.g. pruned after 30 days while the page was in the back stack
        assertEquals(LogDetailState.NotFound, viewModel(99).state.value)
    }
}
