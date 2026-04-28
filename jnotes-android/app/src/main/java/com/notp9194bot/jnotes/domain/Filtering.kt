package com.notp9194bot.jnotes.domain

import com.notp9194bot.jnotes.data.model.FilterParams
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.SmartFilter
import com.notp9194bot.jnotes.data.model.SortBy
import com.notp9194bot.jnotes.data.model.ViewMode

object Filtering {

    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    fun apply(notes: List<Note>, params: FilterParams): List<Note> {
        val now = System.currentTimeMillis()
        val byView = notes.filter {
            when (params.view) {
                ViewMode.ALL -> !it.archived && !it.trashed
                ViewMode.PINNED -> it.pinned && !it.archived && !it.trashed
                ViewMode.ARCHIVED -> it.archived && !it.trashed
                ViewMode.TRASH -> it.trashed
            }
        }

        val bySmart = when (params.smartFilter) {
            SmartFilter.REMINDER -> byView.filter { it.reminderAt > 0 }
            SmartFilter.CHECKLIST -> byView.filter { it.type == NoteType.CHECKLIST }
            SmartFilter.RECENT -> byView.filter { now - it.updatedAt < WEEK_MS }
            null -> byView
        }

        val byTag = if (params.activeTag != null)
            bySmart.filter { params.activeTag in it.tags }
        else bySmart

        val byFolder = if (params.activeFolderId != null)
            byTag.filter { it.folderId == params.activeFolderId }
        else byTag

        val q = params.search.trim().lowercase()
        val bySearch = if (q.isEmpty()) byFolder else byFolder.filter { matches(it, q) }

        val sorted = when (params.sortBy) {
            SortBy.UPDATED -> bySearch.sortedByDescending { it.updatedAt }
            SortBy.CREATED -> bySearch.sortedByDescending { it.createdAt }
            SortBy.TITLE_AZ -> bySearch.sortedBy { it.title.lowercase() }
            SortBy.TITLE_ZA -> bySearch.sortedByDescending { it.title.lowercase() }
        }

        // pinned floats to top in non-trash views
        return if (params.view != ViewMode.TRASH) {
            sorted.sortedByDescending { it.pinned }
        } else sorted
    }

    private fun matches(n: Note, q: String): Boolean {
        if (n.title.lowercase().contains(q)) return true
        if (n.body.lowercase().contains(q)) return true
        if (n.tags.any { it.lowercase().contains(q) }) return true
        if (n.items.any { it.text.lowercase().contains(q) }) return true
        return false
    }

    fun allTags(notes: List<Note>): List<String> =
        notes.asSequence()
            .filter { !it.trashed }
            .flatMap { it.tags.asSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
}
