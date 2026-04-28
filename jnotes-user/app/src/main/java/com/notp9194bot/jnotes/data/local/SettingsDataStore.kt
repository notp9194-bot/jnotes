package com.notp9194bot.jnotes.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notp9194bot.jnotes.data.model.Layout
import com.notp9194bot.jnotes.data.model.Settings
import com.notp9194bot.jnotes.data.model.SortBy
import com.notp9194bot.jnotes.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = intPreferencesKey("accent")
        val layout = stringPreferencesKey("layout")
        val sort = stringPreferencesKey("sort")
        val purgeDays = intPreferencesKey("purge_days")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val amoled = booleanPreferencesKey("amoled")
        val fontScale = floatPreferencesKey("font_scale")
        val biometric = booleanPreferencesKey("biometric")
        val autoLock = intPreferencesKey("auto_lock_sec")
        val lockOnExit = booleanPreferencesKey("lock_on_exit")
        val screenshotBlock = booleanPreferencesKey("screenshot_block")
        val hideInRecents = booleanPreferencesKey("hide_in_recents")
        val persistentEnabled = booleanPreferencesKey("persistent_enabled")
        val persistentId = stringPreferencesKey("persistent_id")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val autoBackupUri = stringPreferencesKey("auto_backup_uri")
        val webdavUrl = stringPreferencesKey("webdav_url")
        val webdavUser = stringPreferencesKey("webdav_user")
        val webdavPass = stringPreferencesKey("webdav_pass")
        val lastSync = longPreferencesKey("last_sync_at")
        val customAccent = intPreferencesKey("custom_accent_argb")
        val language = stringPreferencesKey("language")
        val templates = stringPreferencesKey("custom_templates_json")
        val serverUrl = stringPreferencesKey("server_url")
        val userId = stringPreferencesKey("user_id")
        val displayName = stringPreferencesKey("display_name")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): Settings = Settings(
        theme = runCatching { ThemeMode.valueOf(this[Keys.theme] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM),
        accentIdx = this[Keys.accent] ?: 0,
        layout = runCatching { Layout.valueOf(this[Keys.layout] ?: "GRID") }
            .getOrDefault(Layout.GRID),
        sortBy = runCatching { SortBy.valueOf(this[Keys.sort] ?: "UPDATED") }
            .getOrDefault(SortBy.UPDATED),
        purgeDays = this[Keys.purgeDays] ?: 30,
        pinHash = this[Keys.pinHash],
        pinSalt = this[Keys.pinSalt],
        dynamicColor = this[Keys.dynamicColor] ?: true,
        amoled = this[Keys.amoled] ?: false,
        fontScale = this[Keys.fontScale] ?: 1.0f,
        biometricEnabled = this[Keys.biometric] ?: true,
        autoLockSeconds = this[Keys.autoLock] ?: 0,
        lockOnExit = this[Keys.lockOnExit] ?: false,
        screenshotBlock = this[Keys.screenshotBlock] ?: false,
        hideInRecents = this[Keys.hideInRecents] ?: false,
        persistentNoteEnabled = this[Keys.persistentEnabled] ?: false,
        persistentNoteId = this[Keys.persistentId],
        autoBackupEnabled = this[Keys.autoBackup] ?: false,
        autoBackupFolderUri = this[Keys.autoBackupUri],
        webdavUrl = this[Keys.webdavUrl],
        webdavUser = this[Keys.webdavUser],
        webdavPass = this[Keys.webdavPass],
        lastSyncAt = this[Keys.lastSync] ?: 0L,
        customAccentArgb = this[Keys.customAccent],
        language = this[Keys.language] ?: "system",
        customTemplatesJson = this[Keys.templates] ?: "[]",
        serverUrl = this[Keys.serverUrl] ?: "https://jnotes-ypx2.onrender.com",
        userId = this[Keys.userId] ?: "",
        displayName = this[Keys.displayName] ?: "",
    )

    suspend fun setTheme(mode: ThemeMode) { context.dataStore.edit { it[Keys.theme] = mode.name } }
    suspend fun setAccent(idx: Int) { context.dataStore.edit { it[Keys.accent] = idx } }
    suspend fun setLayout(layout: Layout) { context.dataStore.edit { it[Keys.layout] = layout.name } }
    suspend fun setSort(sort: SortBy) { context.dataStore.edit { it[Keys.sort] = sort.name } }
    suspend fun setPurgeDays(days: Int) { context.dataStore.edit { it[Keys.purgeDays] = days } }
    suspend fun setPinHash(hash: String?, salt: String? = null) {
        context.dataStore.edit { prefs ->
            if (hash == null) {
                prefs.remove(Keys.pinHash); prefs.remove(Keys.pinSalt)
            } else {
                prefs[Keys.pinHash] = hash
                if (salt != null) prefs[Keys.pinSalt] = salt
            }
        }
    }
    suspend fun setDynamicColor(enabled: Boolean) { context.dataStore.edit { it[Keys.dynamicColor] = enabled } }
    suspend fun setAmoled(enabled: Boolean) { context.dataStore.edit { it[Keys.amoled] = enabled } }
    suspend fun setFontScale(v: Float) { context.dataStore.edit { it[Keys.fontScale] = v } }
    suspend fun setBiometric(enabled: Boolean) { context.dataStore.edit { it[Keys.biometric] = enabled } }
    suspend fun setAutoLock(seconds: Int) { context.dataStore.edit { it[Keys.autoLock] = seconds } }
    suspend fun setLockOnExit(enabled: Boolean) { context.dataStore.edit { it[Keys.lockOnExit] = enabled } }
    suspend fun setScreenshotBlock(enabled: Boolean) { context.dataStore.edit { it[Keys.screenshotBlock] = enabled } }
    suspend fun setHideInRecents(enabled: Boolean) { context.dataStore.edit { it[Keys.hideInRecents] = enabled } }
    suspend fun setServerUrl(url: String) { context.dataStore.edit { it[Keys.serverUrl] = url.trim() } }
    suspend fun setDisplayName(name: String) { context.dataStore.edit { it[Keys.displayName] = name.trim() } }
    suspend fun ensureUserId(): String {
        val cur = context.dataStore.data.map { it[Keys.userId] }.firstOrNull()
        if (!cur.isNullOrBlank()) return cur
        val generated = java.util.UUID.randomUUID().toString()
        context.dataStore.edit { it[Keys.userId] = generated }
        return generated
    }
    suspend fun setPersistent(enabled: Boolean, noteId: String? = null) {
        context.dataStore.edit {
            it[Keys.persistentEnabled] = enabled
            if (noteId != null) it[Keys.persistentId] = noteId
            else if (!enabled) it.remove(Keys.persistentId)
        }
    }
    suspend fun setAutoBackup(enabled: Boolean, folderUri: String? = null) {
        context.dataStore.edit {
            it[Keys.autoBackup] = enabled
            if (folderUri != null) it[Keys.autoBackupUri] = folderUri
        }
    }
    suspend fun setWebDav(url: String?, user: String?, pass: String?) {
        context.dataStore.edit {
            if (url.isNullOrBlank()) it.remove(Keys.webdavUrl) else it[Keys.webdavUrl] = url
            if (user.isNullOrBlank()) it.remove(Keys.webdavUser) else it[Keys.webdavUser] = user
            if (pass.isNullOrBlank()) it.remove(Keys.webdavPass) else it[Keys.webdavPass] = pass
        }
    }
    suspend fun setLastSync(t: Long) { context.dataStore.edit { it[Keys.lastSync] = t } }
    suspend fun setCustomAccent(argb: Int?) {
        context.dataStore.edit { if (argb == null) it.remove(Keys.customAccent) else it[Keys.customAccent] = argb }
    }
    suspend fun setLanguage(code: String) { context.dataStore.edit { it[Keys.language] = code } }
    suspend fun setCustomTemplatesJson(json: String) { context.dataStore.edit { it[Keys.templates] = json } }
}
