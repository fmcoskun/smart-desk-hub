package com.iot.widget.ui.cameras

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class CamerasScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val tiles = listOf(
        CameraTileUi("cam_desk", "Desk Camera", FeedState.PRIVACY, "live_desk"),
        CameraTileUi("cam_door", "Door Camera", FeedState.LIVE, "live_door"),
        CameraTileUi("cam_meeting", "Meeting Room Camera", FeedState.OFFLINE, "live_meeting"),
        CameraTileUi("cam_hallway", "Hallway Camera", FeedState.LIVE, "live_hallway"),
    )

    private fun show(state: CamerasUiState, opened: MutableList<String> = mutableListOf()) = rule.setContent {
        // the video slot is empty here: no ExoPlayer in unit tests
        SmartDeskTheme { CamerasContent(state, onOpen = { opened += it }, onRecordings = {}, video = { _, m -> Box(m) }) }
    }

    @Test
    fun tilesShowFeedStates() {
        show(CamerasUiState(tiles, "17:21:34", canViewRecordings = true))
        rule.onNodeWithText("Privacy Guard – feed off").assertExists()
        rule.onNodeWithText("Offline").assertExists()
        rule.onNodeWithText("Recordings").assertExists()
    }

    @Test
    fun tapTileOpensCamera() {
        val opened = mutableListOf<String>()
        show(CamerasUiState(tiles, "17:21:34", canViewRecordings = true), opened)
        rule.onNodeWithText("Door Camera").performClick()
        rule.waitForIdle()
        assertEquals(listOf("cam_door"), opened)
    }

    @Test
    fun guestSeesCamerasButNoRecordingsButton() {
        show(CamerasUiState(tiles, "17:21:34", canViewRecordings = false))
        rule.onNodeWithText("Door Camera").assertExists()
        rule.onNodeWithText("Recordings").assertDoesNotExist()
    }
}
