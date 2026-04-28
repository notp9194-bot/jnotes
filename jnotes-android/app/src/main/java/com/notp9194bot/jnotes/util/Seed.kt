package com.notp9194bot.jnotes.util

import com.notp9194bot.jnotes.data.model.ChecklistItem
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.newNoteId
import com.notp9194bot.jnotes.data.repository.NotesRepository

object Seed {
    suspend fun seed(repo: NotesRepository) {
        for (n in defaults()) repo.upsert(n)
    }

    fun defaults(): List<Note> {
        val now = System.currentTimeMillis()
        fun id() = newNoteId()
        return listOf(
            Note(
                id = id(),
                title = "Welcome to jnotes",
                body = """# Welcome to jnotes

A fast, offline notes app built natively for Android.

- Markdown supported
- *Italic*, **bold**, `inline code`
- Tap a note to open
- Use the **+** button to add a note""",
                type = NoteType.TEXT,
                colorIdx = 0,
                tags = listOf("welcome"),
                pinned = true,
                createdAt = now - 5_000,
                updatedAt = now - 5_000,
            ),
            Note(
                id = id(),
                title = "Groceries",
                type = NoteType.CHECKLIST,
                colorIdx = 2,
                tags = listOf("shopping"),
                items = listOf(
                    ChecklistItem(id = id(), text = "Milk", checked = false),
                    ChecklistItem(id = id(), text = "Bread", checked = true),
                    ChecklistItem(id = id(), text = "Eggs", checked = false),
                    ChecklistItem(id = id(), text = "Coffee beans", checked = false),
                ),
                createdAt = now - 10_000,
                updatedAt = now - 10_000,
            ),
            Note(
                id = id(),
                title = "Project Ideas",
                body = """## Side projects

1. Personal CRM
2. Mood tracker
3. Reading log

See [[Welcome to jnotes]] for tips.""",
                type = NoteType.TEXT,
                colorIdx = 4,
                tags = listOf("ideas", "projects"),
                createdAt = now - 15_000,
                updatedAt = now - 15_000,
            ),
            Note(
                id = id(),
                title = "Weekend plans",
                body = "Hike on Saturday morning, brunch with friends, finish reading the novel.",
                type = NoteType.TEXT,
                colorIdx = 1,
                tags = listOf("personal"),
                createdAt = now - 20_000,
                updatedAt = now - 20_000,
            ),
            Note(
                id = id(),
                title = "Daily standup template",
                body = """### Yesterday
- 

### Today
- 

### Blockers
- """,
                type = NoteType.TEXT,
                colorIdx = 5,
                tags = listOf("work", "template"),
                createdAt = now - 25_000,
                updatedAt = now - 25_000,
            ),
        )
    }
}
