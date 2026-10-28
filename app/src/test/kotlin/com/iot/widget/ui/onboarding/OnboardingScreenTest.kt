package com.iot.widget.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OnboardingScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var finished = 0

    private fun show() = rule.setContent { SmartDeskTheme { OnboardingContent(onFinish = { finished++ }) } }

    @Test
    fun nextBecomesGetStartedOnLastPage() {
        show()
        rule.onNodeWithText("Scenes").assertExists()
        repeat(3) { rule.onNodeWithText("Next").performClick(); rule.waitForIdle() }
        rule.onNodeWithText("Logs & roles").assertIsDisplayed()
        rule.onNodeWithText("Next").assertDoesNotExist()
        rule.onNodeWithText("Get started").performClick()
        rule.waitForIdle()
        assertEquals(1, finished)
    }

    @Test
    fun notShownAfterSkip() {
        show()
        rule.onNodeWithText("Skip").performClick()
        rule.waitForIdle()
        assertEquals(1, finished)
    }

    @Test
    fun backGoesToPreviousPage() {
        show()
        rule.onNodeWithText("Next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Device control & details").assertIsDisplayed()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithText("Scenes").assertIsDisplayed()
        assertEquals(0, finished)
    }


    @Test
    fun doubleNextOnPage3ShowsPage4() {
        show()
        repeat(2) { rule.onNodeWithText("Next").performClick(); rule.waitForIdle() }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Next").performClick()
        rule.mainClock.advanceTimeBy(200) // half-way through the page animation
        // a second quick tap on the same button must not skip the last page
        (if (rule.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty()) "Get started" else "Next")
            .let { rule.onNodeWithText(it).performClick() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(0, finished)
        rule.onNodeWithText("Logs & roles").assertIsDisplayed()
    }

    @Test
    fun replayedTourBackOnPage1Finishes() {
        rule.setContent { SmartDeskTheme { OnboardingContent(onFinish = { finished++ }, replay = true) } }
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(1, finished)
    }


    @Test
    fun firstRunBackOnPage1DoesNotSkipTheTour() {
        show()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(0, finished) // Back is left to Android (leaves the app); the tour is not marked seen
    }
}
