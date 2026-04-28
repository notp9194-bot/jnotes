package com.notp9194bot.jnotes.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.notp9194bot.jnotes.data.model.Note

data class NoteWithItems(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val items: List<ChecklistItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "note_id")
    val attachments: List<AttachmentEntity> = emptyList(),
) {
    fun toModel(): Note = note.toModel(items, attachments)
}
