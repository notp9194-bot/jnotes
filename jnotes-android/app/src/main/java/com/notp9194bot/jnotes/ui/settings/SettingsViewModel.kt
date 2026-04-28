package com.notp9194bot.jnotes.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.notp9194bot.jnotes.ServiceLocator
import com.notp9194bot.jnotes.data.model.Layout
import com.notp9194bot.jnotes.data.model.Settings
import com.notp9194bot.jnotes.data.model.SortBy
import com.notp9194bot.jnotes.data.model.ThemeMode
import com.notp9194bot.jnotes.domain.persistent.PersistentNoteService
import com.notp9194bot.jnotes.domain.work.AutoBackupWorker
import com.notp9194bot.jnotes.domain.work.SyncWorker
import com.notp9194bot.jnotes.util.Hashing
import com.notp9194bot.jnotes.util.JsonBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val app: Application) : AndroidViewModel(app) {

    private val store = ServiceLocator.settings(app)
    private val repo = ServiceLocator.repo(app)

    val settings: StateFlow<Settings> = store.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    fun setTheme(t: ThemeMode) { viewModelScope.launch { store.setTheme(t) } }
    fun setAccent(idx: Int) {
        viewModelScope.launch {
            store.setAccent(idx)
            store.setCustomAccent(null)
        }
    }
    fun setLayout(l: Layout) { viewModelScope.launch { store.setLayout(l) } }
    fun setSort(s: SortBy) { viewModelScope.launch { store.setSort(s) } }
    fun setPurgeDays(d: Int) { viewModelScope.launch { store.setPurgeDays(d) } }
    fun setDynamicColor(enabled: Boolean) { viewModelScope.launch { store.setDynamicColor(enabled) } }
    fun setAmoled(enabled: Boolean) { viewModelScope.launch { store.setAmoled(enabled) } }
    fun setFontScale(v: Float) { viewModelScope.launch { store.setFontScale(v) } }
    fun setBiometric(enabled: Boolean) { viewModelScope.launch { store.setBiometric(enabled) } }
    fun setAutoLockSeconds(sec: Int) { viewModelScope.launch { store.setAutoLock(sec) } }
    fun setLockOnExit(enabled: Boolean) { viewModelScope.launch { store.setLockOnExit(enabled) } }
    fun setScreenshotBlock(enabled: Boolean) { viewModelScope.launch { store.setScreenshotBlock(enabled) } }
    fun setHideInRecents(enabled: Boolean) { viewModelScope.launch { store.setHideInRecents(enabled) } }
    fun setLanguage(code: String) { viewModelScope.launch { store.setLanguage(code) } }
    fun setCustomAccent(argb: Int?) { viewModelScope.launch { store.setCustomAccent(argb) } }
    fun setServerUrl(url: String) { viewModelScope.launch { store.setServerUrl(url) } }
    fun setDisplayName(name: String) { viewModelScope.launch { store.setDisplayName(name) } }

    fun setPin(pin: String?) {
        viewModelScope.launch {
            if (pin.isNullOrBlank()) {
                store.setPinHash(null)
            } else {
                val salt = Hashing.newSalt()
                val hash = Hashing.pbkdf2(pin, salt)
                store.setPinHash(hash, salt)
            }
        }
    }

    fun setPersistent(enabled: Boolean, noteId: String? = null) {
        viewModelScope.launch {
            store.setPersistent(enabled, noteId)
            val intent = Intent(app, PersistentNoteService::class.java)
            if (enabled) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) app.startForegroundService(intent)
                    else app.startService(intent)
                }
            } else {
                app.stopService(intent)
            }
        }
    }

    fun setAutoBackup(enabled: Boolean, folderUri: String? = null) {
        viewModelScope.launch {
            store.setAutoBackup(enabled, folderUri)
            if (enabled) AutoBackupWorker.schedule(app) else AutoBackupWorker.cancel(app)
        }
    }

    fun setWebDav(url: String?, user: String?, pass: String?) {
        viewModelScope.launch {
            store.setWebDav(url, user, pass)
            if (!url.isNullOrBlank()) SyncWorker.schedule(app) else SyncWorker.cancel(app)
        }
    }

    suspend fun exportNow(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val notes = repo.observeAll().first()
            val text = JsonBackup.encode(notes)
            app.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    suspend fun importNow(uri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            val text = app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: return@runCatching 0
            val notes = JsonBackup.decode(text)
            repo.importMany(notes)
            notes.size
        }.getOrDefault(0)
    }

    suspend fun importMarkdown(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val n = com.notp9194bot.jnotes.util.MdImportExport.importMarkdown(app, uri) ?: return@runCatching false
            repo.upsert(n)
            true
        }.getOrDefault(false)
    }

    suspend fun exportAllMarkdown(folderTreeUri: Uri): Int = withContext(Dispatchers.IO) {
        val notes = repo.observeAll().first()
        com.notp9194bot.jnotes.util.MdImportExport.exportAll(app, notes, folderTreeUri)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                return SettingsViewModel(app) as T
            }
        }
    }
}
