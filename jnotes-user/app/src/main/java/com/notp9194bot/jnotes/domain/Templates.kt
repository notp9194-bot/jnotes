package com.notp9194bot.jnotes.domain

import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.Template

object Templates {
    val all: List<Template> = listOf(
        Template(
            id = "blank",
            name = "Blank",
            description = "An empty note",
            type = NoteType.TEXT,
            title = "",
            body = "",
        ),
        Template(
            id = "meeting",
            name = "Meeting Notes",
            description = "Agenda, attendees, action items",
            type = NoteType.TEXT,
            title = "Meeting — ",
            body = """### Attendees
- 

### Agenda
1. 

### Notes
- 

### Action Items
- [ ] """,
            tags = listOf("work", "meetings"),
            colorIdx = 5,
        ),
        Template(
            id = "journal",
            name = "Daily Journal",
            description = "Highlights, gratitude, plans",
            type = NoteType.TEXT,
            title = "Journal — ",
            body = """## Highlights
- 

## Gratitude
- 

## Tomorrow
- """,
            tags = listOf("personal", "journal"),
            colorIdx = 1,
        ),
        Template(
            id = "shopping",
            name = "Shopping List",
            description = "A new checklist",
            type = NoteType.CHECKLIST,
            title = "Shopping",
            body = "",
            items = listOf("Milk", "Bread", "Eggs"),
            tags = listOf("shopping"),
            colorIdx = 2,
        ),
        Template(
            id = "todo",
            name = "To-Do List",
            description = "Tasks for today",
            type = NoteType.CHECKLIST,
            title = "Today",
            body = "",
            items = listOf("First task", "Second task", "Third task"),
            tags = listOf("todo"),
            colorIdx = 3,
        ),
        Template(
            id = "book",
            name = "Book Notes",
            description = "Author, key ideas, quotes",
            type = NoteType.TEXT,
            title = "Book — ",
            body = """**Author:** 
**Started:** 
**Finished:** 

## Key Ideas
- 

## Favorite Quotes
> """,
            tags = listOf("reading"),
            colorIdx = 4,
        ),
    )
}
