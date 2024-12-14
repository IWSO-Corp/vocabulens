package com.iwsocorp.vocabnotes.core.data.repository

import androidx.paging.PagingData
import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import com.iwsocorp.vocabnotes.core.database.model.NoteEntity
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
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

    override suspend fun getNotes(): Flow<List<Note>> {
        val notes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
        val newList = mutableListOf(
            Note(
                id = "",
                title = "All Vocabulary",
                wordLang = "",
                meaningLang = "",
                contentSize = 0,
                createdAt = 0L,
                updatedAt = 0L
            )
        )
        return notes.map {
            it.map { entity ->
                entity.asExternalModel()
            }.forEach {
                newList.add(it)
            }
            newList.toList()
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
    suspend fun getNotes(): Flow<List<Note>>
}