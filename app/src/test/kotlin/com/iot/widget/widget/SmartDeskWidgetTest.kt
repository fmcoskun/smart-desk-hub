package com.iot.widget.widget

import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.MainActivity
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Scene
import com.iot.widget.testutil.SEED_TEST_USERS
import com.iot.widget.testutil.defaultDeskState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The home-screen widget: what it shows for each role and device state. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SmartDeskWidgetTest {

    private val admin = SEED_TEST_USERS.first { it.id == "ayse" }
    private val guest = SEED_TEST_USERS.first { it.id == "guest" }

    private fun offlinePlug() = defaultDeskState().let { s ->
        s.copy(devices = s.devices + (DeviceId.SmartPlug to s.devices.getValue(DeviceId.SmartPlug).copy(online = false)))
    }

    @Test
    fun widgetUiMapsRolesAndOffline() {
        val ui = widgetUi(offlinePlug(), admin)
        assertEquals(WidgetMode.CONTROL, ui.mode)
        assertEquals(listOf(DeviceId.Soundbar, DeviceId.DeskLight, DeviceId.SmartPlug, DeviceId.PrivacyGuard), ui.rows.map { it.id })
        assertEquals(false, ui.rows.first { it.id == DeviceId.SmartPlug }.online)
        assertEquals(WidgetMode.GUEST, widgetUi(defaultDeskState(), guest).mode)
        assertEquals(WidgetMode.SIGNED_OUT, widgetUi(defaultDeskState(), null).mode)
    }

    @Test
    fun showsScenesAndFourSwitches() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(defaultDeskState(), admin)) }
        listOf("Work", "Meet", "Leave", "Bluetooth", "Desk Halo LED", "Smart Plug", "Privacy Guard").forEach {
            onNode(hasText(it)).assertExists()
        }
        onAllNodes(hasText("ON")).assertCountEquals(3) // soundbar, LED and plug start on
    }

    @Test
    fun offlineRowShowsOffline() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(offlinePlug(), admin)) }
        onNode(hasText("Offline")).assertExists()
    }

    @Test
    fun signedOutWidgetIsReadOnly() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(defaultDeskState(), null)) }
        onNode(hasText("Open the app to sign in")).assertExists()
    }

    @Test
    fun guestWidgetIsReadOnly() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(defaultDeskState(), guest)) }
        onNode(hasText("View only (Guest)")).assertExists()
    }

    @Test
    fun activeSceneIsMarked() {
        val desk = defaultDeskState().let { it.copy(settings = it.settings.copy(activeScene = Scene.Leave)) }
        assertEquals(Scene.Leave, widgetUi(desk, admin).activeScene)
    }

    private val openApp = android.content.Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)

    @Test
    fun guestTapsOpenTheApp() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(defaultDeskState(), guest)) }
        // every tap (3 scenes + 4 switches) opens the app instead of sending a command
        onAllNodes(hasStartActivityClickAction(openApp)).assertCountEquals(7)
    }

    @Test
    fun signedOutTapsOpenTheApp() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(defaultDeskState(), null)) }
        onAllNodes(hasStartActivityClickAction(openApp)).assertCountEquals(7)
    }

    @Test
    fun adminTapsDoNotJustOpenTheApp() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        provideComposable { WidgetContent(widgetUi(defaultDeskState(), admin)) }
        onAllNodes(hasStartActivityClickAction(openApp)).assertCountEquals(0) // they run commands
    }
}
