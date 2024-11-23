package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val corpusRepository: CorpusRepository,
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

    override suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun getNoteById(id: String): Note {
        val noteCorpus = corpusRepository.getCorpusByNoteId(id).first()
        Timber.d("note: ${noteDao.getNoteById(id).createdAt}")
        return noteDao.getNoteById(id).let {
            Note(
                id = it.id,
                title = it.title,
                wordLang = it.wordLang,
                meaningLang = it.meaningLang,
                content = noteCorpus,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    override suspend fun getNotes(): Flow<List<Note>> {
        return noteDao.getAllNotes().map { noteEntities ->
            noteEntities.map {
                val noteCorpus = corpusRepository.getCorpusByNoteId(it.id).first()
                Note(
                    id = it.id,
                    title = it.title,
                    wordLang = it.wordLang,
                    meaningLang = it.meaningLang,
                    content = noteCorpus,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }
    }

}

interface NoteRepository {
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun updateUpdatedAt(id: String, updatedAt: Long)
    suspend fun deleteNote(id: String)
    suspend fun getNoteById(id: String): Note
    suspend fun getNotes(): Flow<List<Note>>
}