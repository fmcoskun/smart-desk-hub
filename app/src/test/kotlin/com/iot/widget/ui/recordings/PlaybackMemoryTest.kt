package com.iot.widget.ui.recordings

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Rotating the phone recreates the screen; where the recording was must survive it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlaybackMemoryTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun survivesRecreation() {
        val tester = StateRestorationTester(rule)
        lateinit var memory: PlaybackMemory
        tester.setContent { memory = rememberPlaybackMemory() }
        rule.runOnIdle {
            memory.positionMs = 23_000
            memory.speed = 2f
            memory.playing = false
        }
        tester.emulateSavedInstanceStateRestore()
        rule.runOnIdle {
            assertEquals(23_000L, memory.positionMs)
            assertEquals(2f, memory.speed)
            assertFalse(memory.playing)
        }
    }
}
