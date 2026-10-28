package com.iot.widget.ui.dashboard

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.domain.DetailType
import com.iot.widget.simulation.SensorSimulator
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeDeviceRepository
import com.iot.widget.testutil.FakeScheduler
import com.iot.widget.testutil.FakeSessionRepository
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.fixedClock
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DashboardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val opened = mutableListOf<DetailType>()

    private fun show() {
        val vm = DashboardViewModel(FakeDeskRepository(), RecordingPublisher(), SensorSimulator(Random(1)), FakeScheduler(), FakeSessionRepository(), FakeDeviceRepository(), fixedClock(), Random(2))
        rule.setContent {
            SmartDeskTheme { DashboardScreen(onOpenDetail = { opened += it }, viewModel = vm) }
        }
        rule.waitForIdle()
    }

    @Test
    fun tapLeave_switchesShowOff() {
        show()
        rule.onNodeWithText("Leave").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Bluetooth").assertIsOff()
        rule.onNodeWithContentDescription("Desk Halo LED").assertIsOff()
        rule.onNodeWithContentDescription("Smart Plug").assertIsOff()
        rule.onNodeWithContentDescription("Privacy Guard").assertIsOn()
    }

    @Test
    fun idleConsoleShowsReadyJsonWithQuotes() {
        show()
        rule.onNodeWithText("""office/desk/status -> {"state":"READY"}""", substring = true).assertExists()
    }

    @Test
    fun tapCardBody_opensDetail_tapSwitch_doesNot() {
        show()
        rule.onNodeWithText("Philips Hue ScreenBar").performClick()
        rule.onNodeWithContentDescription("Smart Plug").performClick()
        rule.waitForIdle()
        assertEquals(listOf(DetailType.Light), opened)
    }
}
