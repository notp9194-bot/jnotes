package com.notp9194bot.jnotesadmin.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.notp9194bot.jnotesadmin.MainActivity
import com.notp9194bot.jnotesadmin.R
import com.notp9194bot.jnotesadmin.data.AdminPrefs
import com.notp9194bot.jnotesadmin.data.JnotesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Foreground service that polls the relay every few seconds for new
 * user → admin chat messages and new feedback. When something arrives
 * it pushes an OS notification, even when the admin Activity is killed.
 *
 * It maintains tiny in-memory "seen" sets keyed by message/feedback id
 * so the same item is never notified twice.
 */
class AdminPollingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val seenMessageIds = HashSet<String>()
    private val seenFeedbackIds = HashSet<String>()
    private var primed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannels(this)
        startForeground(NOTIF_ID_ONGOING, buildOngoingNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job?.isActive != true) {
            job = scope.launch { poll() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        scope.coroutineContext[Job]?.cancel()
    }

    private suspend fun poll() {
        val prefs = AdminPrefs(applicationContext)
        while (true) {
            val cfg = prefs.flow.firstOrNull()
            val url = cfg?.serverUrl?.takeIf { it.isNotBlank() }
            val token = cfg?.adminToken?.takeIf { it.isNotBlank() }
            if (url != null && token != null) {
                val api = JnotesApi(url, token)
                runCatching { api.listThreads() }.getOrNull()?.let { threads ->
                    for (t in threads) {
                        val msgs = runCatching { api.getMessages(t.userId) }.getOrNull().orEmpty()
                        for (m in msgs) {
                            if (m.from != "user") continue
                            if (seenMessageIds.add(m.id) && primed) {
                                notifyMessage(t.name.ifBlank { "User" }, m.text)
                            }
                        }
                    }
                }
                runCatching { api.listFeedback() }.getOrNull()?.let { items ->
                    for (f in items) {
                        if (seenFeedbackIds.add(f.id) && primed) {
                            notifyFeedback(f.name.ifBlank { "Anon" }, f.text)
                        }
                    }
                }
                // First pass only seeds the seen-sets — never notify on
                // existing history when the service first starts.
                primed = true
            }
            delay(5_000L)
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun buildOngoingNotification(): Notification {
        return NotificationCompat.Builder(this, CH_ONGOING)
            .setContentTitle("jnotes Admin")
            .setContentText("Listening for new messages and feedback")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppPendingIntent())
            .build()
    }

    private fun notifyMessage(from: String, text: String) {
        val n = NotificationCompat.Builder(this, CH_INBOX)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Message from $from")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()
        getSystemService(NotificationManager::class.java)
            ?.notify(genId(), n)
    }

    private fun notifyFeedback(from: String, text: String) {
        val n = NotificationCompat.Builder(this, CH_INBOX)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Feedback from $from")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent())
            .build()
        getSystemService(NotificationManager::class.java)
            ?.notify(genId(), n)
    }

    companion object {
        private const val CH_ONGOING = "jnotes_admin_ongoing"
        private const val CH_INBOX = "jnotes_admin_inbox"
        private const val NOTIF_ID_ONGOING = 0xA001
        private var nextId = 0xB000
        private fun genId(): Int = ++nextId

        fun start(context: Context) {
            val intent = Intent(context, AdminPollingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CH_ONGOING) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CH_ONGOING,
                        "Background poller",
                        NotificationManager.IMPORTANCE_MIN,
                    ).apply { description = "Keeps the admin app listening for new messages." },
                )
            }
            if (nm.getNotificationChannel(CH_INBOX) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CH_INBOX,
                        "New messages & feedback",
                        NotificationManager.IMPORTANCE_HIGH,
                    ).apply { description = "Notifies when users message the admin or submit feedback." },
                )
            }
        }
    }
}
