package com.notp9194bot.jnotes

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.work.Configuration
import com.notp9194bot.jnotes.domain.persistent.PersistentNoteService
import com.notp9194bot.jnotes.domain.reminder.ReminderChannels
import com.notp9194bot.jnotes.domain.reminder.ReminderScheduler
import com.notp9194bot.jnotes.domain.work.AutoBackupWorker
import com.notp9194bot.jnotes.domain.work.SyncWorker
import com.notp9194bot.jnotes.domain.work.TrashPurgeWorker
import com.notp9194bot.jnotes.util.Seed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class JNotesApp : Application(), Configuration.Provider {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ReminderChannels.ensure(this)
        TrashPurgeWorker.schedule(this)
        seedAndReschedule()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun seedAndReschedule() {
        val repo = ServiceLocator.repo(this)
        appScope.launch {
            val notes = repo.observeAll().first()
            if (notes.isEmpty()) Seed.seed(repo)

            val now = System.currentTimeMillis()
            for (n in repo.notesWithReminders()) {
                if (n.reminderAt > now) {
                    ReminderScheduler.schedule(
                        context = this@JNotesApp,
                        noteId = n.id,
                        title = n.title.ifBlank { "Reminder" },
                        body = n.body,
                        atMillis = n.reminderAt,
                    )
                }
            }

            val s = ServiceLocator.settings(this@JNotesApp).flow.first()
            if (s.autoBackupEnabled) AutoBackupWorker.schedule(this@JNotesApp)
            if (!s.webdavUrl.isNullOrBlank()) SyncWorker.schedule(this@JNotesApp)
            if (s.persistentNoteEnabled) {
                val intent = Intent(this@JNotesApp, PersistentNoteService::class.java)
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                }
            }
        }
    }
}
