package com.notp9194bot.jnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attachments",
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
data class AttachmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("note_id") val noteId: String,
    @ColumnInfo("uri") val uri: String,
    @ColumnInfo("kind") val kind: String, // image | audio | file
    @ColumnInfo("created_at") val createdAt: Long,
)
