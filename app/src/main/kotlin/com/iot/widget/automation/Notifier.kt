package com.iot.widget.automation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iot.widget.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Automation layer · tells the user that an automation acted ("Smart Plug turned off by timer"). */
interface Notifier {
    fun notify(text: String)
}

/**
 * Android notification implementation. Android 8+ needs a *channel* (users can mute it in
 * Settings); Android 13+ also needs the POST_NOTIFICATIONS permission — without it this
 * silently does nothing, and the automation itself still runs.
 */
class AndroidNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : Notifier {

    override fun notify(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_desk)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setColor(context.getColor(R.color.desk_green))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(text.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "automation"
    }
}
