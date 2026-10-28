package com.iot.widget.ui.users

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UserEditScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private var deletes = 0

    private fun show(state: UserEditUiState) = rule.setContent {
        SmartDeskTheme {
            UserEditContent(
                state = state,
                onBack = {},
                onName = {},
                onRole = {},
                onActive = {},
                onSave = {},
                onDelete = { deletes++ },
            )
        }
    }

    @Test
    fun deleteAsksForConfirmation() {
        show(UserEditUiState(loaded = true, name = "Mehmet Kaya", role = Role.OPERATOR, active = true, isNew = false, isMe = false))
        rule.onNodeWithText("Delete user").performScrollTo().performClick()
        rule.onNodeWithText("Delete Mehmet Kaya?").assertExists()
        assertEquals(0, deletes)
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        assertEquals(1, deletes)
    }

    @Test
    fun noDeleteForNewUserOrSelf() {
        show(UserEditUiState(loaded = true, name = "Ayşe Yılmaz", role = Role.ADMIN, active = true, isNew = false, isMe = true))
        rule.onNodeWithText("Delete user").assertDoesNotExist()
    }

    @Test
    fun errorMessageIsShown() {
        show(UserEditUiState(loaded = true, name = "", role = Role.GUEST, active = true, isNew = true, isMe = false, error = RuleViolation.LAST_ADMIN))
        rule.onNodeWithText("At least one active administrator must remain.").assertExists()
    }
}
