package com.iot.widget.ui.login

import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.FakeUserRepository
import com.iot.widget.testutil.RecordingEventLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val users = FakeUserRepository()
    private val session = FakeSessionRepository(users, startUserId = null)
    private val log = RecordingEventLog()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun listsSeededAccountsInOrder() = runTest(dispatcher) {
        val vm = LoginViewModel(users, session, log)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        val accounts = vm.uiState.value.accounts
        assertEquals(listOf("AY", "MK", "ZD", "G"), accounts.map { it.initials })
        assertEquals(listOf(true, true, false, true), accounts.map { it.active })
    }

    @Test
    fun signInRecordsEvent() = runTest(dispatcher) {
        val vm = LoginViewModel(users, session, log)
        vm.signIn("mehmet")
        runCurrent()
        assertEquals("mehmet", session.userId.value)
        assertEquals(listOf("Mehmet Kaya: signed in"), log.lines)
    }

    @Test
    fun inactiveAccountIsNotSignedIn() = runTest(dispatcher) {
        val vm = LoginViewModel(users, session, log)
        vm.signIn("zeynep")
        runCurrent()
        assertEquals(null, session.userId.value)
        assertEquals(emptyList<String>(), log.lines)
    }
}
