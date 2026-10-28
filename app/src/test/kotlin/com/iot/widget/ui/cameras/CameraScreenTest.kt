package com.iot.widget.ui.cameras

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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
class CameraScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show(state: CameraUiState, events: MutableList<String> = mutableListOf()) = rule.setContent {
        SmartDeskTheme {
            CameraContent(
                state,
                onBack = { events += "back" },
                onSnapshot = { events += "snapshot" },
                onRecordings = { events += "recordings" },
                video = { Box(it) },
            )
        }
    }

    @Test
    fun liveCameraSnapshotAndRecordings() {
        val events = mutableListOf<String>()
        show(CameraUiState(CameraTileUi("cam_door", "Door Camera", FeedState.LIVE, "live_door"), "17:21:34", canViewRecordings = true), events)
        rule.onNode(hasText("Snapshot") and hasClickAction()).performClick()
        rule.onNode(hasText("Recordings") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(listOf("snapshot", "recordings"), events)
    }

    @Test
    fun snapshotDisabledWhenOffline() {
        show(CameraUiState(CameraTileUi("cam_door", "Door Camera", FeedState.OFFLINE, "live_door"), "17:21:34"))
        rule.onNode(hasText("Snapshot") and hasClickAction()).assertIsNotEnabled()
        rule.onNodeWithText("Recordings").assertDoesNotExist()
    }

    @Test
    fun snapshotConfirmationShownOnlyForANewSnapshot() {
        val tile = CameraTileUi("cam_door", "Door Camera", FeedState.LIVE, "live_door")
        var state by androidx.compose.runtime.mutableStateOf(CameraUiState(tile, "17:21:34", snapshots = 1))
        rule.setContent {
            SmartDeskTheme { CameraContent(state, onBack = {}, onSnapshot = {}, onRecordings = {}, video = { Box(it) }) }
        }
        // coming back to the page (Back from Recordings, rotation) with an old count: no message
        rule.onNodeWithText("Snapshot saved").assertDoesNotExist()
        state = state.copy(snapshots = 2) // a new snapshot
        rule.waitForIdle()
        rule.onNodeWithText("Snapshot saved").assertExists()
    }
}
