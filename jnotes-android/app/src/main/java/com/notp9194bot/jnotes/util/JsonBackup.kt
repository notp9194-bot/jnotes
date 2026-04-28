package com.notp9194bot.jnotes.util

import com.notp9194bot.jnotes.data.model.Note
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupFile(
    val version: Int = 1,
    val app: String = "jnotes",
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<Note>,
)

object JsonBackup {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(notes: List<Note>): String =
        json.encodeToString(BackupFile.serializer(), BackupFile(notes = notes))

    fun decode(text: String): List<Note> {
        return runCatching {
            json.decodeFromString(BackupFile.serializer(), text).notes
        }.recoverCatching {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Note.serializer()), text)
        }.getOrDefault(emptyList())
    }
}
