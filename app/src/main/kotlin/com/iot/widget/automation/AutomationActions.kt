package com.iot.widget.automation

import android.content.Context
import com.iot.widget.R
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.mqtt.Actors
import com.iot.widget.data.mqtt.MqttMessage
import com.iot.widget.data.mqtt.MqttPublisher
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Payloads
import com.iot.widget.domain.PlugTimer
import com.iot.widget.domain.Topics
import com.iot.widget.domain.isOn
import com.iot.widget.domain.isReachable
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.ZonedDateTime
import javax.inject.Inject

/**
 * Automation layer · what each automation actually does when its moment comes. Shared by the
 * two ways a moment can arrive: a WorkManager worker (no exact-alarm permission) or an exact
 * alarm (AutomationAlarmReceiver). Keeping the logic here means both paths behave identically.
 */
class AutomationActions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: DeskRepository,
    private val publisher: MqttPublisher,
    private val notifier: Notifier,
    private val scheduler: AutomationScheduler,
    private val clock: Clock,
) {
    /** Entry point for exact alarms: the alarm's name says which automation fired. */
    suspend fun onAlarm(name: String, endsAt: Long?) {
        when (name) {
            WorkManagerAutomationScheduler.PLUG_TIMER -> plugTimerFired(endsAt ?: -1)
            WorkManagerAutomationScheduler.LIGHT_ON -> lightScheduleFired(turnOn = true)
            WorkManagerAutomationScheduler.LIGHT_OFF -> lightScheduleFired(turnOn = false)
            WorkManagerAutomationScheduler.SUNSET_DIM -> sunsetFired()
        }
    }

    /**
     * Smart Plug auto-off. Stale guard: the job/alarm carries the end time it was planned for;
     * if the user re-set or cancelled the timer since, the saved end time differs → do nothing.
     */
    suspend fun plugTimerFired(plannedEnd: Long) {
        // check ("is this still the planned end?") and change happen in one SQL step
        if (!repo.turnOffPlugIfPlanned(plannedEnd)) return
        publish(Topics.PLUG, Payloads.toggle(false, now()))
        publish(Topics.PLUG_TIMER, Payloads.value(PlugTimer.Off.key))
        notifier.notify(context.getString(R.string.notif_plug_timer))
    }

    /** LED schedule: switch the light, then plan only THIS action's next occurrence. */
    suspend fun lightScheduleFired(turnOn: Boolean) {
        val desk = repo.current()
        if (!desk.settings.scheduleEnabled) return
        // an offline light gets no command; the next occurrence is still planned below
        if (desk.isReachable(DeviceId.DeskLight) && desk.isOn(DeviceId.DeskLight) != turnOn) {
            repo.setDeviceOn(DeviceId.DeskLight, turnOn)
            publish(Topics.LIGHTS, Payloads.toggle(turnOn, now()))
            notifier.notify(context.getString(if (turnOn) R.string.notif_light_on else R.string.notif_light_off))
        }
        // Re-plan only this action. Re-planning both would append a second copy of the other
        // WorkManager job behind its still-pending one, and appended jobs start their delay late.
        scheduler.scheduleLightAction(turnOn, repo.current().settings, SchedulePolicy.AfterCurrent)
    }

    /** Sunset: a lit LED drops to 30 %, then tomorrow's sunset is planned. */
    suspend fun sunsetFired() {
        val desk = repo.current()
        val lit = desk.isReachable(DeviceId.DeskLight) && desk.isOn(DeviceId.DeskLight)
        if (desk.settings.autoDim && lit && desk.settings.brightness > DIM_LEVEL) {
            repo.updateSettings { it.copy(brightness = DIM_LEVEL) }
            publish(Topics.LIGHTS_DIM, Payloads.brightness(DIM_LEVEL))
            notifier.notify(context.getString(R.string.notif_sunset))
        }
        scheduler.scheduleSunset(repo.current().settings, SchedulePolicy.AfterCurrent)
    }

    private fun publish(topic: String, json: String) = publisher.publish(MqttMessage(topic, json, Actors.SCHEDULER))

    private fun now(): ZonedDateTime = ZonedDateTime.now(clock)

    companion object {
        const val DIM_LEVEL = 30
    }
}
