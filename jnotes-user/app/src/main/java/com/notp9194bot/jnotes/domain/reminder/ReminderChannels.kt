package com.notp9194bot.jnotes.domain.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.notp9194bot.jnotes.R

object ReminderChannels {
    const val ID = "jnotes_reminders"
    const val ID_PERSISTENT = "jnotes_sticky"

    fun ensure(context: Context) {
        val nm = context.getSystemService<NotificationManager>() ?: return
        if (nm.getNotificationChannel(ID) == null) {
            val ch = NotificationChannel(
                ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            nm.createNotificationChannel(ch)
        }
        if (nm.getNotificationChannel(ID_PERSISTENT) == null) {
            val ch = NotificationChannel(
                ID_PERSISTENT,
                context.getString(R.string.persistent_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.persistent_channel_description)
                enableVibration(false)
                setShowBadge(false)
            }
            nm.createNotificationChannel(ch)
        }
    }
}
