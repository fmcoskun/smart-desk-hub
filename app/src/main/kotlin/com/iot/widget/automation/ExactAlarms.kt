package com.iot.widget.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Automation layer · exact alarms (spec §9c). An alarm wakes the phone at the exact moment even
 * in Doze, which WorkManager does not promise. Android 13+ only allows exact alarms after the
 * user grants "Alarms & reminders" in Settings, hence [canSchedule].
 */
interface ExactAlarms {
    fun canSchedule(): Boolean

    /** Sets (or moves) the alarm called [name]; returns false if Android refused it. */
    fun set(name: String, atMillis: Long, endsAt: Long?): Boolean

    fun cancel(name: String)
}

/**
 * AlarmManager implementation. Each automation has one alarm, identified by its name: the
 * PendingIntent (a "token" that lets Android start our receiver later) uses the name as both
 * action and request code, so setting it again replaces the previous one.
 */
class AndroidExactAlarms @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExactAlarms {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun canSchedule(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun set(name: String, atMillis: Long, endsAt: Long?): Boolean = try {
        // ...AndAllowWhileIdle: fires even in Doze. RTC_WAKEUP: wall-clock time, wakes the CPU.
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent(name, endsAt))
        true
    } catch (e: SecurityException) {
        false // permission revoked between canSchedule() and now
    }

    override fun cancel(name: String) = alarmManager.cancel(pendingIntent(name, null))

    private fun pendingIntent(name: String, endsAt: Long?): PendingIntent {
        val intent = Intent(context, AutomationAlarmReceiver::class.java).setAction(name)
        if (endsAt != null) intent.putExtra(AutomationAlarmReceiver.EXTRA_ENDS_AT, endsAt)
        return PendingIntent.getBroadcast(
            context,
            name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
