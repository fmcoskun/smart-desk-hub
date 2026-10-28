package com.iot.widget.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DetailType
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Smart Plug / LED details explain late automations and link to the permission (spec §9c). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExactTimingNoticeTest {

    @get:Rule
    val rule = createComposeRule()

    private val state = DetailUiState(DetailType.Rig, "Smart Plug", deviceOn = true, settings = DeskSettings())

    @Test
    fun noticeShownWithoutPermissionAndAllowOpensSettings() {
        var asked = 0
        rule.setContent {
            SmartDeskTheme { Column { PlugDetail(state, onTimer = {}, exactAllowed = false, onAllowExact = { asked++ }) } }
        }
        rule.onNodeWithText("Allow").performClick()
        rule.waitForIdle()
        assertEquals(1, asked)
    }

    @Test
    fun noticeHiddenWithPermission() {
        rule.setContent {
            SmartDeskTheme { Column { PlugDetail(state, onTimer = {}, exactAllowed = true, onAllowExact = {}) } }
        }
        rule.onNodeWithText("Allow").assertDoesNotExist()
    }
}
