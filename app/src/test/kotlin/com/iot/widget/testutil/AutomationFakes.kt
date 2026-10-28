package com.iot.widget.testutil

import com.iot.widget.automation.AutomationScheduler
import com.iot.widget.automation.Notifier
import com.iot.widget.automation.SchedulePolicy
import com.iot.widget.domain.DeskSettings
import com.iot.widget.domain.PlugTimer
import java.time.Instant

class FakeNotifier : Notifier {
    val texts = mutableListOf<String>()
    override fun notify(text: String) {
        texts += text
    }
}

/** Records calls instead of talking to WorkManager. */
class FakeScheduler : AutomationScheduler {
    val calls = mutableListOf<String>()
    override fun startPlugTimer(timer: PlugTimer, now: Instant): Long {
        calls += "start:${timer.key}"
        return now.toEpochMilli() + (timer.minutes ?: 0) * 60_000L
    }
    override fun cancelPlugTimer() {
        calls += "cancel"
    }
    override fun scheduleLight(settings: DeskSettings, policy: SchedulePolicy) {
        calls += "light:$policy"
    }
    override fun scheduleLightAction(turnOn: Boolean, settings: DeskSettings, policy: SchedulePolicy) {
        calls += "lightAction:${if (turnOn) "on" else "off"}:$policy"
    }
    override fun scheduleSunset(settings: DeskSettings, policy: SchedulePolicy) {
        calls += "sunset:$policy"
    }
    override fun syncAll(settings: DeskSettings) {
        calls += "sync"
    }
}

/** ExactAlarms that records alarms in a map instead of AlarmManager. */
class FakeExactAlarms(var allowed: Boolean) : com.iot.widget.automation.ExactAlarms {
    val alarms = mutableMapOf<String, Pair<Long, Long?>>() // name -> (atMillis, endsAt extra)
    override fun canSchedule(): Boolean = allowed
    override fun set(name: String, atMillis: Long, endsAt: Long?): Boolean {
        alarms[name] = atMillis to endsAt
        return true
    }
    override fun cancel(name: String) {
        alarms.remove(name)
    }
}
