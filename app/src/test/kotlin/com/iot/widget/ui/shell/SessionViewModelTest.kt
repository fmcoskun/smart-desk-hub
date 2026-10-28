package com.iot.widget.ui.shell

import com.iot.widget.testutil.FakeOnboardingRepository
import com.iot.widget.testutil.FakeSessionRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Login → (first time on this device) onboarding → the app. */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val session = FakeSessionRepository(startUserId = null)
    private val onboarding = FakeOnboardingRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): SessionViewModel {
        val vm = SessionViewModel(session, onboarding)
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun shownUntilFinished() = runTest(dispatcher) {
        val vm = viewModel()
        assertEquals(SessionState.SignedOut, vm.state.value)
        session.signIn("ayse")
        runCurrent()
        assertTrue(vm.state.value is SessionState.Onboarding)
        vm.finishOnboarding()
        runCurrent()
        assertEquals("ayse", (vm.state.value as SessionState.SignedIn).user.id)
    }

    @Test
    fun switchUserDoesNotReplay() = runTest(dispatcher) {
        val vm = viewModel()
        session.signIn("ayse")
        vm.finishOnboarding()
        runCurrent()
        session.signOut()
        session.signIn("mehmet")
        runCurrent()
        // once per device, not per user (spec §5)
        assertEquals("mehmet", (vm.state.value as SessionState.SignedIn).user.id)
    }


    @Test
    fun replayedTourReturnsToGuide() = runTest(dispatcher) {
        val vm = viewModel()
        session.signIn("ayse")
        vm.finishOnboarding()
        runCurrent()
        onboarding.reset() // "Show welcome tour" in the User Guide
        runCurrent()
        // the app stays where it was (the Guide) and the tour is shown on top of it
        val s = vm.state.value as SessionState.SignedIn
        assertTrue(s.replayTour)
        vm.finishOnboarding()
        runCurrent()
        assertEquals(false, (vm.state.value as SessionState.SignedIn).replayTour)
    }


    @Test
    fun replayTourSurvivesTheAppGoingToTheBackground() = runTest(dispatcher) {
        val vm = SessionViewModel(session, onboarding)
        var screen = backgroundScope.launch(dispatcher) { vm.state.collect {} }
        runCurrent()
        session.signIn("ayse")
        vm.finishOnboarding()
        runCurrent()
        onboarding.reset() // "Show welcome tour"
        runCurrent()
        screen.cancel() // Home: the screen stops collecting
        advanceTimeBy(10_000)
        screen = backgroundScope.launch(dispatcher) { vm.state.collect {} } // back to the app
        runCurrent()
        val s = vm.state.value
        assertTrue("got $s", s is SessionState.SignedIn && s.replayTour)
    }
}
