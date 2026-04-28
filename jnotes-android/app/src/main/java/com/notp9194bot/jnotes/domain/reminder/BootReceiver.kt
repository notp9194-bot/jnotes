package com.notp9194bot.jnotes.domain.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notp9194bot.jnotes.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pendingResult = goAsync()
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val notes = ServiceLocator.repo(context).notesWithReminders()
                for (n in notes) {
                    if (n.reminderAt > now) {
                        ReminderScheduler.schedule(
                            context = context,
                            noteId = n.id,
                            title = n.title.ifBlank { "Reminder" },
                            body = n.body,
                            atMillis = n.reminderAt,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
