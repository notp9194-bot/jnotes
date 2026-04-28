package com.notp9194bot.jnotes.domain.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService

object ReminderScheduler {

    const val EXTRA_NOTE_ID = "noteId"
    const val EXTRA_TITLE = "noteTitle"
    const val EXTRA_BODY = "noteBody"

    fun schedule(context: Context, noteId: String, title: String, body: String, atMillis: Long) {
        if (atMillis <= System.currentTimeMillis()) return
        val am = context.getSystemService<AlarmManager>() ?: return
        ReminderChannels.ensure(context)
        val pi = pendingIntent(context, noteId, title, body)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, atMillis, pi)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    fun cancel(context: Context, noteId: String) {
        val am = context.getSystemService<AlarmManager>() ?: return
        am.cancel(pendingIntent(context, noteId, "", ""))
    }

    private fun pendingIntent(
        context: Context,
        noteId: String,
        title: String,
        body: String,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, noteId.hashCode(), intent, flags)
    }

    fun extras(intent: Intent): Triple<String?, String?, String?> = Triple(
        intent.getStringExtra(EXTRA_NOTE_ID),
        intent.getStringExtra(EXTRA_TITLE),
        intent.getStringExtra(EXTRA_BODY),
    )
}
