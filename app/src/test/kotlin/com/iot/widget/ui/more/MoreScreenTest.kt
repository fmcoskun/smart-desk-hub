package com.iot.widget.ui.more

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.domain.users.Role
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MoreScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show(role: Role, canManageUsers: Boolean) = rule.setContent {
        SmartDeskTheme {
            MoreContent(
                state = MoreUiState(userName = "Test User", initials = "TU", role = role, canManageUsers = canManageUsers),
                onUsers = { calls += "users" },
                onSwitchUser = { calls += "switch" },
                onAbout = { calls += "about" },
                onGuide = { calls += "guide" },
            )
        }
    }

    @Test
    fun guestSeesNoUsersRow() {
        show(Role.GUEST, canManageUsers = false)
        rule.onNodeWithText("Users").assertDoesNotExist()
        rule.onNodeWithText("Switch user").performClick()
        rule.waitForIdle()
        assertEquals(listOf("switch"), calls)
    }

    @Test
    fun everyoneSeesUserGuide() {
        show(Role.GUEST, canManageUsers = false)
        rule.onNodeWithText("User Guide").performClick()
        rule.waitForIdle()
        assertEquals(listOf("guide"), calls)
    }

    @Test
    fun adminSeesUsersRow() {
        show(Role.ADMIN, canManageUsers = true)
        rule.onNodeWithText("Users").performClick()
        rule.waitForIdle()
        assertEquals(listOf("users"), calls)
    }
}
