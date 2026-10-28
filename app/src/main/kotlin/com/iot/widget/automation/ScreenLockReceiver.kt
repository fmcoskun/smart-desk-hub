package com.iot.widget.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iot.widget.R
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.Actors
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Payloads
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.domain.isReachable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Automation layer · listens for "screen turned off" (= the phone was locked).
 *
 * Android only delivers this broadcast to receivers registered *in code* (not to a manifest
 * receiver), and since Android 14 it holds broadcasts back while an app is "cached" — which
 * happens shortly after the app leaves the screen. So "Enable when screen locks" acts at once
 * while the app is open; a lock that happens while it sits in the background is applied when
 * the user comes back (verified on the API 37 emulator). Acting immediately in the background
 * would need a foreground service with a permanent notification (spec §9b).
 */
class ScreenLockReceiver(private val onScreenOff: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) onScreenOff()
    }
}

/** What happens on lock: Privacy Guard turns on if "Enable when screen locks" is set. */
class ScreenLockHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: DeskRepository,
    private val publisher: MqttPublisher,
    private val notifier: Notifier,
    private val clock: Clock,
) {
    suspend fun onScreenOff() {
        val desk = repo.current()
        if (!desk.settings.privacyOnLock || desk.isOn(DeviceId.PrivacyGuard)) return
        if (!desk.isReachable(DeviceId.PrivacyGuard)) return // offline: it cannot close the shutter
        repo.setDeviceOn(DeviceId.PrivacyGuard, true)
        publisher.publish(MqttMessage(Topics.WIDGET_EVENT, Payloads.notice(Payloads.PRIVACY_NOTICE), Actors.SYSTEM))
        publisher.publish(MqttMessage(Topics.PRIVACY, Payloads.toggle(true, ZonedDateTime.now(clock)), Actors.SYSTEM))
        notifier.notify(context.getString(R.string.notif_privacy_lock))
    }
}
