package com.iot.widget.ui.users

import androidx.lifecycle.SavedStateHandle
import com.iot.widget.domain.RelativeTime
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.FakeUserRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** User list and user editor ViewModels (spec §6.5). Signed in as Ayşe (Admin) unless stated. */
@OptIn(ExperimentalCoroutinesApi::class)
class UserManagerTest {

    private val dispatcher = StandardTestDispatcher()
    private val clock = fixedClock()
    // suspendReads: two quick saves can interleave exactly as they would with Room
    private val users = FakeUserRepository(suspendReads = true)
    private val log = RecordingEventLog()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.editor(id: String?, signedIn: String = "ayse"): UserEditViewModel {
        val handle = SavedStateHandle(if (id == null) emptyMap() else mapOf("id" to id))
        val vm = UserEditViewModel(handle, users, FakeSessionRepository(users, signedIn), log)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun rowsShowRelativeLastLoginAndMe() = runTest(dispatcher) {
        users.markLoggedIn("mehmet", clock.millis() - 2 * 60 * 60_000L)
        val vm = UsersViewModel(users, FakeSessionRepository(users, "ayse"), clock)
        backgroundScope.launch(dispatcher) { vm.uiState.collect {} }
        runCurrent()
        val rows = vm.uiState.value.rows
        assertTrue(rows.first { it.id == "ayse" }.isMe)
        assertEquals(RelativeTime.HoursAgo(2), rows.first { it.id == "mehmet" }.lastLogin)
        assertEquals(RelativeTime.Never, rows.first { it.id == "guest" }.lastLogin)
        assertEquals("ZD", rows.first { it.id == "zeynep" }.initials)
    }

    @Test
    fun createUserSavesAndLogs() = runTest(dispatcher) {
        val vm = editor(id = null)
        vm.setName("  Can Öztürk ")
        vm.setRole(Role.OPERATOR)
        vm.save()
        runCurrent()
        val created = users.flow.value.last()
        assertEquals("Can Öztürk", created.name)
        assertEquals(Role.OPERATOR, created.role)
        assertTrue(vm.uiState.value.saved)
        assertEquals(listOf("Ayşe Yılmaz: created user Can Öztürk"), log.lines)
    }

    @Test
    fun saveBlockedShowsReason() = runTest(dispatcher) {
        val vm = editor(id = "ayse")
        vm.setRole(Role.OPERATOR)
        vm.save()
        runCurrent()
        assertEquals(RuleViolation.SELF_DEMOTE, vm.uiState.value.error)
        assertFalse(vm.uiState.value.saved)
        assertEquals(Role.ADMIN, users.flow.value.first { it.id == "ayse" }.role)
        assertTrue(log.lines.isEmpty())
    }

    @Test
    fun deleteOtherUser() = runTest(dispatcher) {
        val vm = editor(id = "mehmet")
        vm.delete()
        runCurrent()
        assertNull(users.flow.value.firstOrNull { it.id == "mehmet" })
        assertTrue(vm.uiState.value.deleted)
        assertEquals(listOf("Ayşe Yılmaz: deleted user Mehmet Kaya"), log.lines)
    }

    @Test
    fun nonAdminCannotSaveUser() = runTest(dispatcher) {
        val vm = editor(id = "zeynep", signedIn = "mehmet")
        vm.setActive(true)
        vm.save()
        runCurrent()
        assertEquals(RuleViolation.NOT_ALLOWED, vm.uiState.value.error)
        assertFalse(users.flow.value.first { it.id == "zeynep" }.active)
    }

    @Test
    fun duplicateNameRejected() = runTest(dispatcher) {
        val vm = editor(id = null)
        vm.setName("mehmet kaya")
        vm.save()
        runCurrent()
        assertEquals(RuleViolation.NAME_TAKEN, vm.uiState.value.error)
        assertEquals(4, users.flow.value.size)
    }

    @Test
    fun doubleTapOnSaveCreatesOneUser() = runTest(dispatcher) {
        val vm = editor(id = null)
        vm.setName("Deniz Aksoy")
        vm.save()
        vm.save() // second tap before the first save finished
        runCurrent()
        assertEquals(5, users.flow.value.size)
        assertEquals(1, log.lines.size)
    }

    @Test
    fun editingAfterAnErrorClearsIt() = runTest(dispatcher) {
        val vm = editor(id = null)
        vm.save() // empty name
        runCurrent()
        assertNotNull(vm.uiState.value.error)
        vm.setName("Deniz")
        assertNull(vm.uiState.value.error)
    }


    @Test
    fun editingDeletedUserDoesNotRecreateIt() = runTest(dispatcher) {
        val vm = editor("mehmet")
        users.delete("mehmet")
        vm.setName("Mehmet K.")
        vm.save()
        runCurrent()
        assertTrue(users.flow.value.none { it.name == "Mehmet K." })
        assertEquals(RuleViolation.USER_GONE, vm.uiState.value.error)
    }


    @Test
    fun operatorCannotDeleteUser() = runTest(dispatcher) {
        val vm = editor(id = "guest", signedIn = "mehmet")
        vm.delete()
        runCurrent()
        assertTrue(users.flow.value.any { it.id == "guest" })
        assertTrue(log.lines.isEmpty())
    }


    @Test
    fun unknownUserShowsNotFound() = runTest(dispatcher) {
        // an old link or a user deleted on another screen: say so instead of an empty form
        val vm = editor("nobody")
        assertTrue(vm.uiState.value.notFound)
    }
}
