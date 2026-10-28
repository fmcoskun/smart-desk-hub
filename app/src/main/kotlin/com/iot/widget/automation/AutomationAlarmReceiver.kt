package com.iot.widget.automation

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iot.widget.data.DeskRepository
import com.iot.widget.di.ApplicationScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Hilt cannot construct a BroadcastReceiver (Android creates it), so receivers ask the app's
 * dependency container for what they need through an @EntryPoint interface.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutomationEntryPoint {
    fun actions(): AutomationActions
    fun scheduler(): AutomationScheduler
    fun repository(): DeskRepository

    @ApplicationScope
    fun scope(): CoroutineScope
}

/**
 * Automation layer · an exact alarm went off: run that automation right now.
 *
 * goAsync() tells Android "I am still working" so the process is not frozen while the
 * coroutine finishes the (short) database write; finish() releases it.
 */
class AutomationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.action ?: return
        val endsAt = if (intent.hasExtra(EXTRA_ENDS_AT)) intent.getLongExtra(EXTRA_ENDS_AT, -1) else null
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, AutomationEntryPoint::class.java)
        val pending = goAsync()
        deps.scope().launch {
            try {
                deps.actions().onAlarm(name, endsAt)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ENDS_AT = "ends_at"
    }
}

/**
 * Automation layer · system events after which every automation must be planned again:
 * - the phone restarted (Android deletes all alarms on reboot),
 * - the app was updated,
 * - the user granted or revoked "Alarms & reminders" (switches between alarms and WorkManager).
 * These actions can only be sent by the system, so exporting this receiver is safe.
 */
class AutomationSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESYNC_ACTIONS) return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, AutomationEntryPoint::class.java)
        val pending = goAsync()
        deps.scope().launch {
            try {
                deps.scheduler().syncAll(deps.repository().current().settings)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESYNC_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
