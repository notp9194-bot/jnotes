package com.notp9194bot.jnotes.data.model

import androidx.compose.runtime.Immutable

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class Layout { LIST, GRID }
enum class SortBy { UPDATED, CREATED, TITLE_AZ, TITLE_ZA }

@Immutable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accentIdx: Int = 0,
    val layout: Layout = Layout.GRID,
    val sortBy: SortBy = SortBy.UPDATED,
    val purgeDays: Int = 30,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
    val fontScale: Float = 1.0f,

    // ── Privacy & security ──────────────────────────────────────────────────
    /** Use fingerprint/face/device-credential as alternative to PIN. ON by default so it just works once a PIN is set. */
    val biometricEnabled: Boolean = true,
    /** Auto-lock the app after N seconds in background. 0 = never. */
    val autoLockSeconds: Int = 0,
    /** Re-lock the app the moment it goes to background, regardless of timer. */
    val lockOnExit: Boolean = false,
    /** Block screenshots and screen recording (FLAG_SECURE). */
    val screenshotBlock: Boolean = false,
    /** Hide note content in the recent-apps thumbnail. */
    val hideInRecents: Boolean = false,

    val persistentNoteEnabled: Boolean = false,
    val persistentNoteId: String? = null,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolderUri: String? = null,
    val webdavUrl: String? = null,
    val webdavUser: String? = null,
    val webdavPass: String? = null,
    val lastSyncAt: Long = 0L,
    val customAccentArgb: Int? = null,
    val language: String = "system", // system | en | hi
    val customTemplatesJson: String = "[]",

    // ── Admin chat / feedback ──────────────────────────────────────────────
    /** Backend URL (e.g. http://192.168.1.10:8787). Empty disables remote features. */
    val serverUrl: String = "",
    /** Stable per-install identifier — generated lazily, never shown. */
    val userId: String = "",
    /** Optional display name shown to the admin. */
    val displayName: String = "",
)
