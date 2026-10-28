package com.iot.widget.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DetailType
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Rotating the phone while editing the LED schedule keeps the editor open. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LightDetailRotationTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun scheduleDraftSurvivesRotation() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            SmartDeskTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    LightDetail(DetailUiState(DetailType.Light, "Desk Halo LED", true, DeskSettings()), {}, {}, { _, _, _, _, _ -> })
                }
            }
        }
        rule.onNodeWithText("Turn on").performScrollTo().performClick()
        rule.onNodeWithText("Turn on at").assertExists()
        tester.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Turn on at").assertExists()
    }
}

