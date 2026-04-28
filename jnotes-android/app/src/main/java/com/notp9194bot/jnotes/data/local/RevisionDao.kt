package com.notp9194bot.jnotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RevisionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(revision: NoteRevisionEntity)

    @Query("SELECT * FROM note_revisions WHERE note_id = :noteId ORDER BY created_at DESC LIMIT :limit")
    suspend fun forNote(noteId: String, limit: Int = 50): List<NoteRevisionEntity>

    @Query("DELETE FROM note_revisions WHERE note_id = :noteId AND id NOT IN (SELECT id FROM note_revisions WHERE note_id = :noteId ORDER BY created_at DESC LIMIT :keep)")
    suspend fun trimOld(noteId: String, keep: Int = 30)
}
