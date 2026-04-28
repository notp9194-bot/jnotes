package com.notp9194bot.jnotes.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_revisions",
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
data class NoteRevisionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("note_id") val noteId: String,
    @ColumnInfo("title") val title: String,
    @ColumnInfo("body") val body: String,
    @ColumnInfo("created_at") val createdAt: Long,
)
