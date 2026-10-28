package com.iot.widget.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Behaviour of the reusable neumorphic controls (rendering is checked on the emulator). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ComponentsTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun neuSwitch_clickTogglesAndShowsLabel() {
        var on by mutableStateOf(false)
        rule.setContent {
            SmartDeskTheme {
                NeuSwitch(checked = on, onCheckedChange = { on = it }, contentDescription = "Bluetooth")
            }
        }
        rule.onNodeWithText("OFF").assertExists()
        rule.onNodeWithContentDescription("Bluetooth").performClick()
        rule.waitForIdle()
        assertTrue(on)
        rule.onNodeWithText("ON").assertExists()
    }

    @Test
    fun neuSwitch_nullCallbackIsDisabled() {
        rule.setContent {
            SmartDeskTheme { NeuSwitch(checked = true, onCheckedChange = null, contentDescription = "Plug") }
        }
        rule.onNodeWithContentDescription("Plug").assertIsNotEnabled()
    }

    @Test
    fun neuSwitch_touchTargetAtLeast44dp() {
        rule.setContent {
            SmartDeskTheme { NeuSwitch(checked = false, onCheckedChange = {}, contentDescription = "Light") }
        }
        rule.onNodeWithContentDescription("Light").assertHeightIsAtLeast(44.dp)
    }

    @Test
    fun neuSegmentedAndChip_touchTargetsAtLeast44dp() {
        rule.setContent {
            SmartDeskTheme {
                androidx.compose.foundation.layout.Column {
                    NeuSegmented(
                        options = listOf("a" to "Alpha", "b" to "Beta"),
                        selected = "a",
                        onSelect = {},
                    )
                    NeuChipButton(text = "Work", icon = null, active = true, onClick = {})
                }
            }
        }
        rule.onNodeWithText("Beta").assertHeightIsAtLeast(44.dp)
        rule.onNodeWithText("Work").assertHeightIsAtLeast(44.dp)
    }

    @Test
    fun neuSegmented_selectsOption() {
        var picked: String? = null
        rule.setContent {
            SmartDeskTheme {
                NeuSegmented(
                    options = listOf("2700" to "Warm", "4000" to "Neutral", "6500" to "Cool"),
                    selected = "4000",
                    onSelect = { picked = it },
                )
            }
        }
        rule.onNodeWithText("Cool").performClick()
        rule.waitForIdle()
        assertEquals("6500", picked)
    }
}
