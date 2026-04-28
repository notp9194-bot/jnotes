package com.notp9194bot.jnotes.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class NoteType { TEXT, CHECKLIST }

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

@Immutable
@Serializable
data class Note(
    val id: String,
    val title: String = "",
    val body: String = "",
    val type: NoteType = NoteType.TEXT,
    val items: List<ChecklistItem> = emptyList(),
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val trashedAt: Long = 0L,
    val colorIdx: Int = 0,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderAt: Long = 0L,
    val locked: Boolean = false,
    val folderId: String? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val recurrenceInterval: Int = 1,
    val attachmentUris: List<String> = emptyList(),
)

fun Note.preview(max: Int = 200): String {
    if (type == NoteType.CHECKLIST) {
        val total = items.size
        val done = items.count { it.checked }
        val first = items.asSequence()
            .filter { !it.checked }
            .map { it.text }
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" · ")
        return first.ifBlank { "$done / $total done" }
    }
    return body.replace(Regex("[#*_>`\\[\\]]"), "").take(max)
}

fun Note.wordCount(): Int = when (type) {
    NoteType.CHECKLIST -> items.sumOf { it.text.trim().split(Regex("\\s+")).filter { w -> w.isNotEmpty() }.size }
    NoteType.TEXT -> {
        val s = (title + " " + body).trim()
        if (s.isEmpty()) 0 else s.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }
}

fun Note.readingTimeMinutes(): Int {
    val w = wordCount()
    return ((w + 199) / 200).coerceAtLeast(1)
}

fun newNoteId(): String =
    java.lang.Long.toString(System.currentTimeMillis(), 36) +
        java.util.UUID.randomUUID().toString().substring(0, 6)
