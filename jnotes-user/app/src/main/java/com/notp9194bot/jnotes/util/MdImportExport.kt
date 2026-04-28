package com.notp9194bot.jnotes.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.notp9194bot.jnotes.data.model.ChecklistItem
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.newNoteId

object MdImportExport {

    /** Low-visibility "jnotes" footer auto-appended to every exported markdown file. */
    private const val WATERMARK_FOOTER = "\n\n---\n<sub><i>— exported from jnotes</i></sub>\n"

    fun exportSingle(context: Context, note: Note, target: Uri): Boolean = runCatching {
        context.contentResolver.openOutputStream(target)?.use { os ->
            os.write(toMarkdown(note, watermark = true).toByteArray(Charsets.UTF_8))
        }
        true
    }.getOrDefault(false)

    fun exportAll(context: Context, notes: List<Note>, folderTreeUri: Uri): Int {
        val folder = DocumentFile.fromTreeUri(context, folderTreeUri) ?: return 0
        var count = 0
        for (n in notes) {
            val name = (n.title.ifBlank { "untitled-${n.id}" })
                .replace(Regex("[^A-Za-z0-9 _-]"), "_")
                .take(80)
            val file = folder.createFile("text/markdown", "$name.md") ?: continue
            context.contentResolver.openOutputStream(file.uri)?.use {
                it.write(toMarkdown(n, watermark = true).toByteArray(Charsets.UTF_8))
            }
            count++
        }
        return count
    }

    fun importMarkdown(context: Context, source: Uri): Note? {
        val text = context.contentResolver.openInputStream(source)?.bufferedReader()?.use { it.readText() }
            ?: return null
        return parseMarkdown(text)
    }

    /**
     * Renders a note as Markdown. When [watermark] is true (default for exports),
     * a tiny low-key "exported from jnotes" footer is appended.
     */
    fun toMarkdown(note: Note, watermark: Boolean = false): String = buildString {
        if (note.title.isNotBlank()) appendLine("# ${note.title}").appendLine()
        if (note.tags.isNotEmpty()) appendLine(note.tags.joinToString(" ") { "#$it" }).appendLine()
        if (note.type == NoteType.CHECKLIST) {
            note.items.forEach { i ->
                appendLine("- [${if (i.checked) "x" else " "}] ${i.text}")
            }
        } else {
            append(note.body)
        }
        if (watermark) append(WATERMARK_FOOTER)
    }

    fun parseMarkdown(text: String): Note {
        // Strip trailing watermark when re-importing exported jnotes files so it
        // does not pollute the body text.
        val cleaned = text.replace(WATERMARK_FOOTER, "").trimEnd()
        val lines = cleaned.lines()
        var title = ""
        val bodyLines = mutableListOf<String>()
        val items = mutableListOf<ChecklistItem>()
        var foundTitle = false
        var anyChecklist = false
        for (line in lines) {
            if (!foundTitle && line.startsWith("# ")) {
                title = line.removePrefix("# ").trim()
                foundTitle = true
                continue
            }
            val task = Regex("^- \\[( |x|X)\\] (.*)").matchEntire(line)
            if (task != null) {
                anyChecklist = true
                items += ChecklistItem(
                    id = newNoteId(),
                    text = task.groupValues[2].trim(),
                    checked = task.groupValues[1].equals("x", ignoreCase = true),
                )
            } else {
                bodyLines += line
            }
        }
        return if (anyChecklist) {
            Note(id = newNoteId(), title = title, type = NoteType.CHECKLIST, items = items)
        } else {
            Note(id = newNoteId(), title = title, body = bodyLines.joinToString("\n").trim())
        }
    }
}
