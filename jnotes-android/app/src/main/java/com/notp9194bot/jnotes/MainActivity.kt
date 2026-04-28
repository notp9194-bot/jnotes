package com.notp9194bot.jnotes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.newNoteId
import com.notp9194bot.jnotes.ui.lock.LockManager
import com.notp9194bot.jnotes.ui.nav.AppNavHost
import com.notp9194bot.jnotes.ui.theme.JNotesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* no-op */ }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val deepNoteId = handleIncomingIntent(intent)

        // React to privacy toggles in real time: FLAG_SECURE for screenshot/screen
        // recording protection AND for hiding the recents thumbnail.
        lifecycleScope.launch {
            ServiceLocator.settings(this@MainActivity).flow.collectLatest { s ->
                val secure = s.screenshotBlock || s.hideInRecents || (s.pinHash != null && s.lockOnExit)
                if (secure) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE,
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }

        setContent {
            val settings by ServiceLocator.settings(this).flow.collectAsState(
                initial = com.notp9194bot.jnotes.data.model.Settings(),
            )
            JNotesTheme(settings = settings) {
                AppNavHost(initialNoteId = deepNoteId)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Record the moment the app went to background — used by AppNavHost
        // to decide whether the auto-lock timer has elapsed when we resume.
        LockManager.markBackgrounded()
    }

    /**
     * Returns the noteId to open after launch (either deep-link or a freshly-created share-IN note).
     */
    private fun handleIncomingIntent(intent: Intent?): String? {
        if (intent == null) return null
        val action = intent.action

        // Deep-link from reminder/widget
        intent.getStringExtra("openNoteId")?.let { return it }

        // Static shortcuts
        when (action) {
            "com.notp9194bot.jnotes.NEW_TEXT" -> return createBlankAndReturnId(NoteType.TEXT)
            "com.notp9194bot.jnotes.NEW_CHECK" -> return createBlankAndReturnId(NoteType.CHECKLIST)
        }

        // Share-IN (text/plain) — create a new note from the shared text
        if (action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
            val body = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            if (title.isNotBlank() || body.isNotBlank()) {
                val id = newNoteId()
                val note = com.notp9194bot.jnotes.data.model.Note(id = id, title = title, body = body)
                ioScope.launch { ServiceLocator.repo(this@MainActivity).upsert(note) }
                return id
            }
        }

        // Share-IN (single image) — create note with image attachment URI inserted as markdown
        if (action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                val id = newNoteId()
                val note = com.notp9194bot.jnotes.data.model.Note(
                    id = id,
                    title = "Shared image",
                    body = "![image]($uri)",
                )
                ioScope.launch {
                    val repo = ServiceLocator.repo(this@MainActivity)
                    repo.upsert(note)
                    repo.addAttachment(id, uri.toString(), kind = "image")
                }
                return id
            }
        }
        return null
    }

    private fun createBlankAndReturnId(type: NoteType): String {
        val id = newNoteId()
        val note = com.notp9194bot.jnotes.data.model.Note(id = id, type = type)
        ioScope.launch { ServiceLocator.repo(this@MainActivity).upsert(note) }
        return id
    }
}
