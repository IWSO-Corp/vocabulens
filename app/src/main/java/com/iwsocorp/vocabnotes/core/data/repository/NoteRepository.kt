package com.iwsocorp.vocabnotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertHeaderItem
import androidx.paging.map
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

    override suspend fun incrementContentSize(id: String, count: Int) {
        noteDao.incrementContentSize(id, count)
    }

    override suspend fun decrementContentSize(id: String, count: Int) {
        noteDao.decrementContentSize(id, count)
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun getNoteById(id: String): Note {
        return noteDao.getNoteById(id).asExternalModel()
    }

    override suspend fun getNoteList(): List<Note> {
        return noteDao.getNoteList().map { it.asExternalModel() }
    }

    override fun getNotes(): Flow<PagingData<Note>> {
        val staticNote = Note(
            id = "",
            title = "All Vocabulary",
            wordLang = "",
            meaningLang = "",
            contentSize = 0,
            createdAt = 0L,
            updatedAt = 0L
        )

        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                noteDao.getAllNotes()
            }
        ).flow.map { pagingData: PagingData<NoteEntity> ->
            pagingData.map {
                it.asExternalModel()
            }.insertHeaderItem(item = staticNote)
        }
    }

}

interface NoteRepository {
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun incrementContentSize(id: String, count: Int)
    suspend fun decrementContentSize(id: String, count: Int)
    suspend fun deleteNote(id: String)
    suspend fun getNoteById(id: String): Note
    suspend fun getNoteList(): List<Note>
    fun getNotes(): Flow<PagingData<Note>>
}