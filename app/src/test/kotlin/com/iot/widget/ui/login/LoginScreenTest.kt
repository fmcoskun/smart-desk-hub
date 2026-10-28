package com.iot.widget.ui.login

import androidx.compose.ui.test.assertIsNotEnabled
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
class LoginScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val accounts = listOf(
        AccountUi("ayse", "Ayşe Yılmaz", Role.ADMIN, active = true, initials = "AY"),
        AccountUi("zeynep", "Zeynep Demir", Role.OPERATOR, active = false, initials = "ZD"),
    )
    private val picked = mutableListOf<String>()

    private fun show() = rule.setContent {
        SmartDeskTheme { LoginContent(LoginUiState(accounts), onPick = { picked += it }) }
    }

    @Test
    fun disabledAccountNotClickable() {
        show()
        rule.onNodeWithText("Zeynep Demir", substring = true).assertIsNotEnabled()
        assertEquals(emptyList<String>(), picked)
    }

    @Test
    fun tapAccountSignsIn() {
        show()
        rule.onNodeWithText("Ayşe Yılmaz", substring = true).performClick()
        rule.waitForIdle()
        assertEquals(listOf("ayse"), picked)
    }
}
