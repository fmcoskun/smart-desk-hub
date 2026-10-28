package com.iot.widget.automation

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.testutil.FakeDeskRepository
import com.iot.widget.testutil.FakeNotifier
import com.iot.widget.testutil.RecordingPublisher
import com.iot.widget.testutil.defaultDeskState
import com.iot.widget.testutil.fixedClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScreenLockReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val publisher = RecordingPublisher()
    private val notifier = FakeNotifier()

    private fun handler(repo: FakeDeskRepository) = ScreenLockHandler(context, repo, publisher, notifier, fixedClock())

    @Test
    fun receiverReactsOnlyToScreenOff() {
        var calls = 0
        val receiver = ScreenLockReceiver { calls++ }
        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))
        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
        assertEquals(1, calls)
    }

    @Test
    fun screenOffEnablesPrivacyWhenSettingOn() = runTest {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(privacyOnLock = true)))
        handler(repo).onScreenOff()
        assertTrue(repo.flow.value.isOn(DeviceId.PrivacyGuard))
        assertEquals(listOf(Topics.WIDGET_EVENT, Topics.PRIVACY), publisher.topics())
        assertEquals(listOf("Privacy Guard enabled: screen locked"), notifier.texts)
    }

    @Test
    fun screenOffDoesNothingWhenSettingOff() = runTest {
        val repo = FakeDeskRepository(defaultDeskState(DeskSettings(privacyOnLock = false)))
        handler(repo).onScreenOff()
        assertFalse(repo.flow.value.isOn(DeviceId.PrivacyGuard))
        assertTrue(publisher.messages.isEmpty())
    }


    @Test
    fun screenLockSkipsOfflinePrivacyGuard() = runTest {
        val start = defaultDeskState(DeskSettings(privacyOnLock = true))
        val guard = start.devices.getValue(DeviceId.PrivacyGuard).copy(online = false)
        val repo = FakeDeskRepository(start.copy(devices = start.devices + (DeviceId.PrivacyGuard to guard)))
        handler(repo).onScreenOff()
        assertFalse(repo.flow.value.isOn(DeviceId.PrivacyGuard))
        assertTrue(publisher.topics().isEmpty())
    }
}
