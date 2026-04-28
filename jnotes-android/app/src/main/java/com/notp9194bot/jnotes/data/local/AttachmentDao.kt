package com.notp9194bot.jnotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM attachments WHERE note_id = :noteId AND uri = :uri")
    suspend fun deleteByUri(noteId: String, uri: String)

    @Query("SELECT * FROM attachments WHERE note_id = :noteId ORDER BY created_at ASC")
    suspend fun forNote(noteId: String): List<AttachmentEntity>
}
