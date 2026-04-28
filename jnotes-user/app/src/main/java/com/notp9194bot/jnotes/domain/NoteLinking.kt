package com.notp9194bot.jnotes.domain

import com.notp9194bot.jnotes.data.model.Note

object NoteLinking {

    private val linkRegex = Regex("\\[\\[([^\\[\\]]+)]]")

    fun outgoingLinks(text: String): List<String> =
        linkRegex.findAll(text).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()

    fun resolveByTitle(notes: List<Note>, title: String): Note? {
        val target = title.trim().lowercase()
        return notes.firstOrNull { it.title.trim().lowercase() == target && !it.trashed }
    }

    fun backlinksFor(target: Note, notes: List<Note>): List<Note> {
        val title = target.title.trim().lowercase()
        if (title.isEmpty()) return emptyList()
        return notes.filter { other ->
            other.id != target.id && !other.trashed &&
                outgoingLinks(other.body).any { it.lowercase() == title }
        }
    }
}
