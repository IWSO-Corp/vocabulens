package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import com.iwsocorp.vocabnotes.core.database.model.NoteEntity
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
) : NoteRepository {

    override suspend fun addNote(note: Note) {
        noteDao.insertNote(note.asEntity())
    }

    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.asEntity())
    }

    override suspend fun updateUpdatedAt(id: String, updatedAt: Long) {
        noteDao.updateUpdatedAt(id, updatedAt)
    }

    override suspend fun updateContentSize(id: String, contentSize: Int) {
        noteDao.updateContentSize(id, contentSize)
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun getNoteById(id: String): Note {
        return noteDao.getNoteById(id).asExternalModel()
    }

    override fun getNotes(): Flow<List<Note>> {
        val notes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
        return notes.map {
            it.map { entity ->
                entity.asExternalModel()
            }
        }
    }

}

interface NoteRepository {
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun updateUpdatedAt(id: String, updatedAt: Long)
    suspend fun updateContentSize(id: String, contentSize: Int)
    suspend fun deleteNote(id: String)
    suspend fun getNoteById(id: String): Note
    fun getNotes(): Flow<List<Note>>
}