package com.notp9194bot.jnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.Recurrence

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("title") val title: String,
    @ColumnInfo("body") val body: String,
    @ColumnInfo("type") val type: String,
    @ColumnInfo("pinned") val pinned: Boolean,
    @ColumnInfo("archived") val archived: Boolean,
    @ColumnInfo("trashed") val trashed: Boolean,
    @ColumnInfo("trashed_at") val trashedAt: Long,
    @ColumnInfo("color_idx") val colorIdx: Int,
    @ColumnInfo("tags") val tags: List<String>,
    @ColumnInfo("created_at") val createdAt: Long,
    @ColumnInfo("updated_at") val updatedAt: Long,
    @ColumnInfo("reminder_at") val reminderAt: Long,
    @ColumnInfo("locked") val locked: Boolean,
    @ColumnInfo(name = "folder_id", defaultValue = "NULL") val folderId: String? = null,
    @ColumnInfo(name = "recurrence_rule", defaultValue = "'NONE'") val recurrenceRule: String = "NONE",
    @ColumnInfo(name = "recurrence_interval", defaultValue = "1") val recurrenceInterval: Int = 1,
)

fun NoteEntity.toModel(items: List<ChecklistItemEntity>, attachments: List<AttachmentEntity> = emptyList()): Note = Note(
    id = id,
    title = title,
    body = body,
    type = if (type == "checklist") NoteType.CHECKLIST else NoteType.TEXT,
    items = items.sortedBy { it.position }.map { it.toModel() },
    pinned = pinned,
    archived = archived,
    trashed = trashed,
    trashedAt = trashedAt,
    colorIdx = colorIdx,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reminderAt = reminderAt,
    locked = locked,
    folderId = folderId,
    recurrence = runCatching { Recurrence.valueOf(recurrenceRule) }.getOrDefault(Recurrence.NONE),
    recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
    attachmentUris = attachments.map { it.uri },
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    body = body,
    type = if (type == NoteType.CHECKLIST) "checklist" else "text",
    pinned = pinned,
    archived = archived,
    trashed = trashed,
    trashedAt = trashedAt,
    colorIdx = colorIdx,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reminderAt = reminderAt,
    locked = locked,
    folderId = folderId,
    recurrenceRule = recurrence.name,
    recurrenceInterval = recurrenceInterval.coerceAtLeast(1),
)
