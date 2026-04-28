package com.notp9194bot.jnotes

import android.content.Context
import com.notp9194bot.jnotes.data.local.AppDatabase
import com.notp9194bot.jnotes.data.local.SettingsDataStore
import com.notp9194bot.jnotes.data.repository.NotesRepository

object ServiceLocator {
    @Volatile private var repo: NotesRepository? = null
    @Volatile private var settings: SettingsDataStore? = null

    fun repo(context: Context): NotesRepository = repo ?: synchronized(this) {
        repo ?: run {
            val db = AppDatabase.get(context)
            NotesRepository(
                dao = db.noteDao(),
                folderDao = db.folderDao(),
                attachmentDao = db.attachmentDao(),
                revisionDao = db.revisionDao(),
            ).also { repo = it }
        }
    }

    fun settings(context: Context): SettingsDataStore = settings ?: synchronized(this) {
        settings ?: SettingsDataStore(context.applicationContext).also { settings = it }
    }
}
