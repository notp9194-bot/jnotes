package com.notp9194bot.jnotes.domain.persistent

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.notp9194bot.jnotes.MainActivity
import com.notp9194bot.jnotes.R
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.preview
import com.notp9194bot.jnotes.domain.reminder.ReminderChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that pins the user's selected (or first pinned) note as a low-priority
 * persistent notification. Re-trigger by starting it again (e.g. on note save).
 */
class PersistentNoteService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val NOTIF_ID = 9001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ReminderChannels.ensure(this)
        startForeground(NOTIF_ID, buildPlaceholder())
        scope.launch { refresh() }
        return START_STICKY
    }

    private suspend fun refresh() {
        val s = ServiceLocator.settings(this).flow.first()
        if (!s.persistentNoteEnabled) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val repo = ServiceLocator.repo(this)
        val note = s.persistentNoteId?.let { repo.getById(it) } ?: repo.firstPinnedNote()
        val title = note?.title?.ifBlank { "Sticky note" } ?: "Sticky note"
        val body = note?.preview() ?: getString(R.string.widget_empty)
        val tap = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (note != null) putExtra("openNoteId", note.id)
        }
        val pi = PendingIntent.getActivity(
            this,
            7,
            tap,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(this, ReminderChannels.ID_PERSISTENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
        startForeground(NOTIF_ID, n)
    }

    private fun buildPlaceholder(): android.app.Notification {
        return NotificationCompat.Builder(this, ReminderChannels.ID_PERSISTENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("jnotes")
            .setContentText("Sticky note loading…")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
