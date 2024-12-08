package com.iwsocorp.vocabnotes.core.data.repository

import android.R.attr.data
import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
        return noteDao.getNoteById(id).asExternalModel(noteCorpus)
    }

    override fun getNotes(): Flow<List<Note>> = flow {
        val notesBatch = mutableListOf<Note>()

        noteDao.getAllNotes().collect { noteEntities ->
            for (noteEntity in noteEntities) {
                val noteCorpus = corpusRepository.getCorpusByNoteId(noteEntity.id).first().take(3)
                val note = noteEntity.asExternalModel(noteCorpus)

                // Add the note to the batch
                notesBatch.add(note)

                // Emit the batch when it reaches 10 items
                if (notesBatch.size == 10) {
                    emit(notesBatch.toList()) // Emit the current batch
                    notesBatch.clear() // Clear the batch for the next 10 items
                }
            }

            // If there are remaining items (less than 10), emit them
            if (notesBatch.isNotEmpty()) {
                emit(notesBatch.toList())
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
    fun getNotes(): Flow<List<Note>>
}