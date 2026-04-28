package com.notp9194bot.jnotes.domain.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.notp9194bot.jnotes.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderActionReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_SNOOZE_10 = "com.notp9194bot.jnotes.SNOOZE_10"
        const val ACTION_SNOOZE_1H = "com.notp9194bot.jnotes.SNOOZE_1H"
        const val ACTION_SNOOZE_TOMORROW = "com.notp9194bot.jnotes.SNOOZE_TOMORROW"
        const val ACTION_DONE = "com.notp9194bot.jnotes.MARK_DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra(ReminderScheduler.EXTRA_NOTE_ID) ?: return
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY).orEmpty()

        context.getSystemService<NotificationManager>()?.cancel(noteId.hashCode())

        val now = System.currentTimeMillis()
        val newAt = when (intent.action) {
            ACTION_SNOOZE_10 -> now + 10L * 60_000L
            ACTION_SNOOZE_1H -> now + 60L * 60_000L
            ACTION_SNOOZE_TOMORROW -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            ACTION_DONE -> 0L
            else -> 0L
        }

        scope.launch {
            ServiceLocator.repo(context).setReminder(noteId, newAt)
            if (newAt > 0L) ReminderScheduler.schedule(context, noteId, title, body, newAt)
        }
    }
}
