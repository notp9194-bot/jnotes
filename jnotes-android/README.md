# jnotes — native Android (v2)

A modern, fast, fully offline native Android notes app built with **Kotlin + Jetpack Compose + Material 3 + Room + WorkManager + DataStore**.

Single-Activity, Compose Navigation, MVVM, Kotlin Coroutines + Flow. Deeply split modular package layout for fast incremental compiles and clean ownership of every concern.

---

## Build

Requires Android Studio Iguana / Hedgehog or newer with Android SDK 34, JDK 17.

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

`minSdk = 26`, `targetSdk = 34`, `applicationId = com.notp9194bot.jnotes`.

---

## Features

### Notes
- Text notes with full **Markdown** preview (headings, bold/italic/strikethrough, inline & fenced code, blockquotes, ordered/unordered/checkbox lists, links, images).
- Markdown formatting toolbar in the editor (B/I/S, code, H1/H2, lists, checkbox, quote, link).
- **Checklists** (drag-reorderable, indent/outdent, complete with strikethrough).
- **Drawing notes** with stroke + color picker.
- Title + body, **autosave** on every keystroke and on app pause.
- **Pin** any note to the top of the list.
- **Color tags** + **labels/tags**.
- **Folders** with quick filter from the navigation drawer; bulk move-to-folder.
- **Reminders** with one-shot or **recurring** schedules (daily / weekly / monthly / yearly + custom interval).
- **Image attachments** via the system picker (inline `![image](uri)` Markdown for text notes).
- **Note linking** with `[[Note title]]` syntax, jumps directly to the linked note.
- **Find & replace** with case-sensitive toggle.
- Title-case helper, word & character count, paste-at-end / clear-body.

### Library
- Two-pane layout on tablets / large screens (auto via `WindowSizeClass`).
- **Pinned section** rendered above the rest.
- **Calendar view** of all upcoming reminders.
- Search (title + body), tag filter, color filter, folder filter.
- Grid/list layout, sort by Updated / Created / Title A→Z / Title Z→A.
- **Trash** with auto-purge after N days (1–90, slider).

### Privacy & lock
- **PIN lock** (4–8 digits, **PBKDF2** with random salt, 120k iterations).
- **Biometric / device-credential** unlock as alternative to PIN (BiometricPrompt).

### Personalization
- Light / Dark / System theme.
- 12 preset accent colors + **custom accent** picker.
- **AMOLED black** mode for dark theme.
- **Material You** dynamic colors (Android 12+).
- **Font scale** slider (0.7×–1.6×).
- Language preference (System / English / हिन्दी).

### Backup & sync
- One-tap **JSON export / import** (full backup).
- **Markdown** import (single .md) and **export-all** (one .md per note into a chosen folder via SAF).
- **Per-note PDF export** (A4, paginated, from the editor).
- **Daily auto-backup** to a SAF folder, keeps the 14 newest snapshots (WorkManager + DocumentFile).
- **WebDAV sync** every 6 hours (Basic auth, GET/PUT `jnotes-backup.json`).

### System integration
- **Persistent note** in the notification shade — keep one note one swipe away (foreground service).
- **Boot receiver** restores reminders after device reboot.
- Deep link from notifications opens the note directly.

---

## Project structure

```
app/src/main/java/com/notp9194bot/jnotes/
  JNotesApp.kt                 // Application: workers, persistent service bootstrap
  MainActivity.kt              // single Activity hosting Compose
  ServiceLocator.kt            // tiny manual DI
  data/
    local/                     // Room DAOs, entities, DataStore
    model/                     // Note, Folder, Settings, enums
    repo/                      // NotesRepository
  domain/
    persistent/                // PersistentNoteService (foreground)
    reminder/                  // BootReceiver, ReminderScheduler
    work/                      // AutoBackupWorker, SyncWorker, PurgeWorker
  ui/
    common/                    // ChecklistEditor, ColorPickerRow, MarkdownView, ReminderDialog
    drawer/                    // AppDrawer
    editor/                    // EditorScreen, EditorViewModel, MarkdownToolbar, FolderPickerDialog
    home/                      // HomeScreen, HomeViewModel, NoteCard, Filtering, FilterParams
    calendar/                  // CalendarScreen
    lock/                      // BiometricGate, PinLockScreen
    nav/                       // AppNavHost (routes incl. CALENDAR)
    settings/                  // SettingsScreen, SettingsViewModel
    theme/                     // Color (12 accents + amoled), Theme (custom accent + font scale), Type
  util/
    DateUtils.kt
    Hashing.kt                 // sha256 (legacy verify) + PBKDF2 + verifyPin
    JsonBackup.kt
    MdImportExport.kt          // single + folder export, parser
    PdfExport.kt               // A4 paginated PDF
    WebDavSync.kt              // Basic auth GET/PUT
```

---

## Notes on what was deliberately left out

These items were considered but skipped on purpose for v2 to keep the build self-contained and dependency-light:

- SQLCipher database encryption.
- ML Kit OCR for handwriting / scanned notes.
- Google Drive / Dropbox OAuth (use **WebDAV** instead — works with Nextcloud, ownCloud, mailbox.org, etc.).
- Wear OS companion.
- Real-time collaboration / multi-device live editing.

The UI surface and data model leave room to add any of these later without rewrites.
