package com.notp9194bot.jnotes.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes")
    fun observeAll(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteWithItems?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE note_id = :noteId")
    suspend fun deleteItemsForNote(noteId: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM notes WHERE trashed = 1 AND trashed_at < :cutoff")
    suspend fun purgeTrashOlderThan(cutoff: Long): Int

    @Query("DELETE FROM notes WHERE trashed = 1")
    suspend fun emptyTrash()

    @Query("UPDATE notes SET reminder_at = :reminderAt, updated_at = :now WHERE id = :id")
    suspend fun setReminder(id: String, reminderAt: Long, now: Long)

    @Query("SELECT * FROM notes WHERE reminder_at > 0")
    suspend fun notesWithReminders(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE pinned = 1 AND trashed = 0 AND archived = 0 ORDER BY updated_at DESC LIMIT 1")
    suspend fun firstPinnedNote(): NoteEntity?

    @Transaction
    suspend fun replaceItems(noteId: String, items: List<ChecklistItemEntity>) {
        deleteItemsForNote(noteId)
        if (items.isNotEmpty()) insertItems(items)
    }
}
