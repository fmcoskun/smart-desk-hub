package com.iot.widget.ui.devices

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iot.widget.domain.devices.DeviceType
import com.iot.widget.domain.devices.Room
import com.iot.widget.ui.theme.SmartDeskTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DevicesScreenTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tapRowOpensDetail_addHiddenForNonAdmin() {
        val opened = mutableListOf<String>()
        val state = DevicesUiState(
            rows = listOf(DeviceRowUi("room_sensor", "Room Sensor", DeviceType.SENSOR, Room.DESK, online = true, summary = DeviceSummary.Battery(78))),
            filter = DeviceFilter.ALL,
            counts = mapOf(DeviceFilter.ALL to 1),
            canAdd = false,
        )
        rule.setContent {
            SmartDeskTheme { DevicesContent(state, onFilter = {}, onOpen = { opened += it }, onAdd = {}) }
        }
        rule.onNodeWithText("Room Sensor").performClick()
        rule.waitForIdle()
        assertEquals(listOf("room_sensor"), opened)
        rule.onNodeWithText("Add device").assertDoesNotExist()
        rule.onNodeWithText("Desk · Battery 78%").assertExists()
    }
}
