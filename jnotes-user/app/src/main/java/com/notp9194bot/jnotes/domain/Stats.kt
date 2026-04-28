package com.notp9194bot.jnotes.domain

import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.wordCount
import com.notp9194bot.jnotes.util.DateUtils

data class StatsSummary(
    val totalNotes: Int,
    val activeNotes: Int,
    val pinned: Int,
    val archived: Int,
    val trashed: Int,
    val checklists: Int,
    val checklistItemsTotal: Int,
    val checklistItemsDone: Int,
    val totalWords: Int,
    val totalChars: Int,
    val tagsUsed: Int,
    val streakDays: Int,
    val sparkline: List<Int>,
    val topTags: List<Pair<String, Int>>,
)

object Stats {

    fun summarize(notes: List<Note>): StatsSummary {
        val active = notes.filter { !it.trashed }
        val pinned = notes.count { it.pinned && !it.trashed }
        val archived = notes.count { it.archived && !it.trashed }
        val trashed = notes.count { it.trashed }
        val checklists = notes.count { it.type == NoteType.CHECKLIST && !it.trashed }
        val items = active.flatMap { it.items }
        val itemsTotal = items.size
        val itemsDone = items.count { it.checked }
        val totalWords = active.sumOf { it.wordCount() }
        val totalChars = active.sumOf { it.title.length + it.body.length + it.items.sumOf { i -> i.text.length } }

        val tagCounts = HashMap<String, Int>()
        for (n in active) for (t in n.tags) tagCounts[t] = (tagCounts[t] ?: 0) + 1
        val topTags = tagCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val now = System.currentTimeMillis()
        val today = DateUtils.startOfDay(now)
        val sparkline = IntArray(14)
        for (n in active) {
            val day = DateUtils.startOfDay(n.updatedAt)
            val diffDays = ((today - day) / 86_400_000L).toInt()
            if (diffDays in 0..13) sparkline[13 - diffDays] += 1
        }

        var streak = 0
        var cursor = today
        val daysWithEdits = active.map { DateUtils.startOfDay(it.updatedAt) }.toHashSet()
        while (cursor in daysWithEdits) {
            streak += 1
            cursor -= 86_400_000L
        }

        return StatsSummary(
            totalNotes = notes.size,
            activeNotes = active.size,
            pinned = pinned,
            archived = archived,
            trashed = trashed,
            checklists = checklists,
            checklistItemsTotal = itemsTotal,
            checklistItemsDone = itemsDone,
            totalWords = totalWords,
            totalChars = totalChars,
            tagsUsed = tagCounts.size,
            streakDays = streak,
            sparkline = sparkline.toList(),
            topTags = topTags,
        )
    }
}
