package com.notp9194bot.jnotes.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.notp9194bot.jnotes.MainActivity
import com.notp9194bot.jnotes.R
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StickyNoteWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> render(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NEW_NOTE) {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_NEW_NOTE_INTENT
            }
            context.startActivity(openIntent)
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, StickyNoteWidget::class.java))
            ids.forEach { render(context, mgr, it) }
        }
    }

    private fun render(context: Context, manager: AppWidgetManager, id: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_sticky)

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context, id, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_body, openPi)
        views.setOnClickPendingIntent(R.id.widget_title, openPi)

        val newIntent = Intent(context, StickyNoteWidget::class.java).apply { action = ACTION_NEW_NOTE }
        val newPi = PendingIntent.getBroadcast(
            context, id + 9000, newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_new_btn, newPi)

        manager.updateAppWidget(id, views)

        scope.launch {
            val s = ServiceLocator.settings(context).flow.first()
            val repo = ServiceLocator.repo(context)
            val note = s.persistentNoteId?.let { repo.getById(it) } ?: repo.firstPinnedNote()
            val title = note?.title?.ifBlank { "jnotes" } ?: "jnotes"
            val body = note?.preview()?.take(180) ?: context.getString(R.string.widget_empty)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_body, body)
            if (note != null) {
                val openNote = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("openNoteId", note.id)
                }
                val pi = PendingIntent.getActivity(
                    context, id + 1000, openNote,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_body, pi)
                views.setOnClickPendingIntent(R.id.widget_title, pi)
            }
            manager.updateAppWidget(id, views)
        }
    }

    companion object {
        const val ACTION_NEW_NOTE = "com.notp9194bot.jnotes.widget.NEW_NOTE"
        const val ACTION_NEW_NOTE_INTENT = "com.notp9194bot.jnotes.NEW_TEXT"

        fun nudge(context: Context) {
            val intent = Intent(context, StickyNoteWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}
