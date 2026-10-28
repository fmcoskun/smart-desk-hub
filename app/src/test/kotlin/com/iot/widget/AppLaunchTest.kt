package com.iot.widget

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke test: the app starts (Hilt graph builds, Compose renders) and, signed out, shows Login.
 * Runs on the JVM with Robolectric, so no emulator is needed.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltTestApplication::class)
class AppLaunchTest {

    // Hilt must be set up before the activity starts, hence order = 0
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_showsLoginWhenSignedOut() {
        // Room/DataStore load off the Compose idle tracking, so wait for the content itself
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("Choose a demo account")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
