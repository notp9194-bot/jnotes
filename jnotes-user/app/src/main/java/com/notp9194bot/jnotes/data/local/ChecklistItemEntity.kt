package com.notp9194bot.jnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.notp9194bot.jnotes.data.model.ChecklistItem

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["note_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("note_id")],
)
data class ChecklistItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("note_id") val noteId: String,
    @ColumnInfo("text") val text: String,
    @ColumnInfo("checked") val checked: Boolean,
    @ColumnInfo("position") val position: Int,
)

fun ChecklistItemEntity.toModel(): ChecklistItem =
    ChecklistItem(id = id, text = text, checked = checked)

fun ChecklistItem.toEntity(noteId: String, position: Int): ChecklistItemEntity =
    ChecklistItemEntity(
        id = id,
        noteId = noteId,
        text = text,
        checked = checked,
        position = position,
    )
