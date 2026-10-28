package com.iot.widget.ui.detail

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Whole-app test: dashboard → detail → back (button and system back). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltTestApplication::class)
class DetailNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun signInAsAdmin() {
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasText("Ayşe Yılmaz")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Ayşe Yılmaz").performClick()
        // fresh install: the welcome tour comes first
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasText("Skip")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Skip").performClick()
    }

    private fun openLightDetail() {
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasText("Philips Hue ScreenBar")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Philips Hue ScreenBar").performClick()
        // The detail content comes from Room, which loads on its own background thread;
        // waitForIdle() does not wait for that, so wait for the content itself
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasText("COLOR TEMPERATURE")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun backButtonAndSystemBackReturnToDashboard() {
        signInAsAdmin()
        openLightDetail()
        rule.onNodeWithText("Back").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("COLOR TEMPERATURE").assertDoesNotExist()

        openLightDetail()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithText("COLOR TEMPERATURE").assertDoesNotExist()

        // A double tap on Back (second tap during the slide-out) must not pop the dashboard too
        openLightDetail()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.advanceTimeBy(50)
        rule.onNodeWithText("Back").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("SMART DESK HUB").assertExists()

        // On the dashboard, back leaves the app
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertTrue(rule.activity.isFinishing)
    }
}
