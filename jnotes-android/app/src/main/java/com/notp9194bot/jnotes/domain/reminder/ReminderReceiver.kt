package com.notp9194bot.jnotes.domain.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.notp9194bot.jnotes.MainActivity
import com.notp9194bot.jnotes.R
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.Recurrence
import com.notp9194bot.jnotes.domain.RecurrenceCalc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        ReminderChannels.ensure(context)
        val (noteId, titleExtra, bodyExtra) = ReminderScheduler.extras(intent)
        if (noteId.isNullOrEmpty()) return

        val title = titleExtra?.takeIf { it.isNotBlank() } ?: "Reminder"
        val body = bodyExtra?.take(120) ?: ""

        val tap = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openNoteId", noteId)
        }
        val pi = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        fun action(label: String, code: String, idOffset: Int) =
            NotificationCompat.Action.Builder(
                0,
                label,
                PendingIntent.getBroadcast(
                    context,
                    noteId.hashCode() + idOffset,
                    Intent(context, ReminderActionReceiver::class.java).apply {
                        action = code
                        putExtra(ReminderScheduler.EXTRA_NOTE_ID, noteId)
                        putExtra(ReminderScheduler.EXTRA_TITLE, title)
                        putExtra(ReminderScheduler.EXTRA_BODY, body)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            ).build()

        val notif = NotificationCompat.Builder(context, ReminderChannels.ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(action(context.getString(R.string.action_snooze_10), ReminderActionReceiver.ACTION_SNOOZE_10, 1001))
            .addAction(action(context.getString(R.string.action_snooze_1h), ReminderActionReceiver.ACTION_SNOOZE_1H, 1002))
            .addAction(action(context.getString(R.string.action_done), ReminderActionReceiver.ACTION_DONE, 1003))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        context.getSystemService<NotificationManager>()?.notify(noteId.hashCode(), notif)

        scope.launch {
            val n = ServiceLocator.repo(context).getById(noteId)
            if (n != null && n.recurrence != Recurrence.NONE) {
                val next = RecurrenceCalc.nextOccurrence(System.currentTimeMillis(), n.recurrence, n.recurrenceInterval)
                if (next != null) {
                    ServiceLocator.repo(context).setReminder(noteId, next)
                    ReminderScheduler.schedule(context, noteId, title, body, next)
                    return@launch
                }
            }
            ServiceLocator.repo(context).setReminder(noteId, 0L)
        }
    }
}
