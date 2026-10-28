package com.iot.widget.ui.shell

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iot.widget.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Whole app: sign in as Guest (view only), switch user, sign in as Admin (user manager visible). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = HiltTestApplication::class)
class ShellNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    // Room and DataStore load off the Compose idle tracking: wait for the content itself
    private fun waitFor(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun guestIsViewOnly_switchUser_adminSeesUsers() {
        waitFor("Guest · view only")
        // the test screen is small: scroll the card into view before tapping it
        rule.onNodeWithText("Guest · view only").performScrollTo().performClick()
        waitFor("Skip") // fresh install: the welcome tour comes first
        rule.onNodeWithText("Skip").performClick()
        waitFor("Philips Hue ScreenBar")
        rule.onNodeWithContentDescription("Desk Halo LED").assertIsNotEnabled()
        rule.onNodeWithText("Logs").assertDoesNotExist() // Guest has no Logs tab
        rule.onNodeWithText("Cameras").assertExists() // every role watches live cameras

        rule.onNodeWithText("More").performClick()
        waitFor("Switch user")
        rule.onNodeWithText("Users").assertDoesNotExist()
        rule.onNodeWithText("Switch user").performClick()

        waitFor("Ayşe Yılmaz")
        rule.onNodeWithText("Ayşe Yılmaz").performClick()
        waitFor("Logs") // Admin has the Logs tab
        rule.onNodeWithText("More").performClick()
        waitFor("Users")
    }

    @Test
    fun viewLogsShortcut_thenDevicesTab_showsTheDeviceList() {
        waitFor("Ayşe Yılmaz")
        rule.onNodeWithText("Ayşe Yılmaz").performScrollTo().performClick()
        waitFor("Skip")
        rule.onNodeWithText("Skip").performClick()
        waitFor("Devices")
        rule.onNodeWithText("Devices").performClick()
        waitFor("Smart Plug")
        rule.onNodeWithText("Smart Plug").performScrollTo().performClick()
        waitFor("View logs")
        rule.onNodeWithText("View logs").performScrollTo().performClick()
        waitFor("Device: Smart Plug")

        // the Devices tab must lead back to the device list, not to the filtered Logs page
        rule.onNodeWithText("Devices").performClick()
        waitFor("Add device")
        rule.onNodeWithText("Device: Smart Plug").assertDoesNotExist()
    }


    @Test
    fun moreUserGuideOpensATopic() {
        waitFor("Ayşe Yılmaz")
        rule.onNodeWithText("Ayşe Yılmaz").performScrollTo().performClick()
        waitFor("Skip")
        rule.onNodeWithText("Skip").performClick()
        waitFor("More")
        rule.onNodeWithText("More").performClick()
        waitFor("User Guide")
        rule.onNodeWithText("User Guide").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Search every action", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Search every action", substring = true).performScrollTo().performClick()
        waitFor("STEPS") // the topic page, loaded through GuideTopicRoute(id) → SavedStateHandle
        rule.onNodeWithText("Export CSV shares", substring = true).assertExists()
    }
}
