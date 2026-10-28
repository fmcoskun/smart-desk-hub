package com.iot.widget.ui.recordings

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iot.widget.domain.cameras.RecordingEvent
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordingsScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val cameras = listOf("cam_desk" to "Desk Camera", "cam_door" to "Door Camera")

    @Test
    fun emptyState() {
        rule.setContent {
            SmartDeskTheme { RecordingsContent(RecordingsUiState(cameras = cameras, allowed = true, loaded = true), RecordingsActions.NONE) }
        }
        rule.onNodeWithText("No recordings for this day").assertExists()
    }

    @Test
    fun rowShowsEventCameraTimeDurationAndOpens() {
        val opened = mutableListOf<Long>()
        val row = RecordingRowUi(1, "Door Camera", "14:05", "0:45", RecordingEvent.PERSON)
        rule.setContent {
            SmartDeskTheme {
                RecordingsContent(
                    RecordingsUiState(rows = listOf(row), cameras = cameras, allowed = true, loaded = true),
                    RecordingsActions.NONE.copy(onOpen = { opened += it }),
                )
            }
        }
        rule.onNodeWithText("PERSON").assertExists()
        rule.onNodeWithText("14:05 · 0:45").performClick()
        rule.waitForIdle()
        assertEquals(listOf(1L), opened)
    }

    @Test
    fun playerControlsCallBack() {
        val events = mutableListOf<String>()
        rule.setContent {
            SmartDeskTheme {
                PlayerContent(
                    info = PlayerInfoUi("rec_45", "Door Camera", "Tue 27 Oct, 16:21:34", 45_000, RecordingEvent.PERSON),
                    position = 12_000,
                    playing = true,
                    speed = 1f,
                    onBack = {},
                    onPlayPause = { events += "toggle" },
                    onSeek = { events += "seek $it" },
                    onSkip = { events += "skip $it" },
                    onSpeed = { events += "speed $it" },
                    video = { Box(it) },
                )
            }
        }
        rule.onNodeWithText("0:12 / 0:45").assertExists()
        rule.onNodeWithContentDescription("Back 10 seconds").performClick()
        rule.onNodeWithContentDescription("Forward 10 seconds").performClick()
        rule.onNodeWithContentDescription("Pause").performClick()
        rule.onNodeWithText("2×").performScrollTo().performClick() // below the fold on the small test screen
        rule.waitForIdle()
        assertEquals(listOf("skip -10000", "skip 10000", "toggle", "speed 2.0"), events)
    }
}
