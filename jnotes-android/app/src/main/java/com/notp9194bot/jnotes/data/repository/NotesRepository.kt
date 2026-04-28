package com.notp9194bot.jnotes.data.repository

import com.notp9194bot.jnotes.data.local.AttachmentDao
import com.notp9194bot.jnotes.data.local.AttachmentEntity
import com.notp9194bot.jnotes.data.local.FolderDao
import com.notp9194bot.jnotes.data.local.FolderEntity
import com.notp9194bot.jnotes.data.local.NoteDao
import com.notp9194bot.jnotes.data.local.NoteRevisionEntity
import com.notp9194bot.jnotes.data.local.RevisionDao
import com.notp9194bot.jnotes.data.local.toEntity
import com.notp9194bot.jnotes.data.local.toModel
import com.notp9194bot.jnotes.data.model.Folder
import com.notp9194bot.jnotes.data.model.Note
import com.notp9194bot.jnotes.data.model.NoteType
import com.notp9194bot.jnotes.data.model.newNoteId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class NotesRepository(
    private val dao: NoteDao,
    private val folderDao: FolderDao,
    private val attachmentDao: AttachmentDao,
    private val revisionDao: RevisionDao,
) {

    fun observeAll(): Flow<List<Note>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    fun observeById(id: String): Flow<Note?> =
        dao.observeById(id).map { it?.toModel() }

    suspend fun getById(id: String): Note? = dao.getById(id)?.toModel()

    fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun upsertFolder(folder: Folder) = folderDao.upsert(folder.toEntity())

    suspend fun deleteFolder(id: String) {
        folderDao.clearReferences(id)
        folderDao.delete(id)
    }

    suspend fun upsert(note: Note, recordRevision: Boolean = false) {
        val now = System.currentTimeMillis()
        val n = note.copy(updatedAt = now)
        val previous = if (recordRevision) dao.getById(n.id)?.toModel() else null
        dao.upsertNote(n.toEntity())
        if (n.type == NoteType.CHECKLIST) {
            val items = n.items.mapIndexedNotNull { idx, item ->
                if (item.text.isBlank() && !item.checked) null
                else item.toEntity(n.id, idx)
            }
            dao.replaceItems(n.id, items)
        } else {
            dao.replaceItems(n.id, emptyList())
        }
        if (previous != null && (previous.title != n.title || previous.body != n.body)) {
            revisionDao.insert(
                NoteRevisionEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = n.id,
                    title = previous.title,
                    body = previous.body,
                    createdAt = now,
                ),
            )
            revisionDao.trimOld(n.id, keep = 30)
        }
    }

    suspend fun pinToggle(note: Note) = upsert(note.copy(pinned = !note.pinned))

    suspend fun setColor(note: Note, idx: Int) = upsert(note.copy(colorIdx = idx))

    suspend fun archive(note: Note) =
        upsert(note.copy(archived = true, pinned = false, trashed = false))

    suspend fun unarchive(note: Note) = upsert(note.copy(archived = false))

    suspend fun trash(note: Note) =
        upsert(note.copy(trashed = true, pinned = false, archived = false, trashedAt = System.currentTimeMillis()))

    suspend fun restore(note: Note) =
        upsert(note.copy(trashed = false, trashedAt = 0L))

    suspend fun deletePermanently(id: String) = dao.deletePermanently(id)

    suspend fun emptyTrash() = dao.emptyTrash()

    suspend fun purgeOlderThan(cutoff: Long): Int = dao.purgeTrashOlderThan(cutoff)

    suspend fun setReminder(id: String, reminderAt: Long) =
        dao.setReminder(id, reminderAt, System.currentTimeMillis())

    suspend fun notesWithReminders(): List<Note> =
        dao.notesWithReminders().map { it.toModel(emptyList()) }

    suspend fun firstPinnedNote(): Note? = dao.firstPinnedNote()?.toModel(emptyList())

    suspend fun newBlank(folderId: String? = null): Note {
        val n = Note(id = newNoteId(), folderId = folderId)
        dao.upsertNote(n.toEntity())
        return n
    }

    suspend fun importMany(notes: List<Note>) {
        notes.forEach { upsert(it) }
    }

    suspend fun addAttachment(noteId: String, uri: String, kind: String) {
        attachmentDao.insert(
            AttachmentEntity(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                uri = uri,
                kind = kind,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun removeAttachment(noteId: String, uri: String) {
        attachmentDao.deleteByUri(noteId, uri)
    }

    suspend fun attachmentsForNote(noteId: String): List<AttachmentEntity> =
        attachmentDao.forNote(noteId)

    suspend fun revisions(noteId: String): List<NoteRevisionEntity> = revisionDao.forNote(noteId)
}
