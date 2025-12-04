package com.iwsocorp.vobynotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.iwsocorp.vobynotes.core.database.dao.NoteDao
import com.iwsocorp.vobynotes.core.database.model.asExternalModel
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.asEntity
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

    override suspend fun updateNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String
    ): Int {
        return noteDao.updateNote(noteId, title, wordLang, meaningLang)
    }

    override suspend fun updateNoteSharedStatus(ids: List<String>, shared: Boolean) {
        noteDao.updateNoteSharedStatus(ids, shared)
    }

    override suspend fun incrementContentSize(id: String, count: Int) {
        noteDao.incrementContentSize(id, count)
    }

    override suspend fun decrementContentSize(id: String, count: Int) {
        noteDao.decrementContentSize(id, count)
    }

    override suspend fun deleteNotes(ids: List<String>) {
        noteDao.deleteNoteByIds(ids)
    }

    override suspend fun getNoteById(id: String): Note {
        return noteDao.getNoteById(id).asExternalModel()
    }

    override fun getNotesFlow(): Flow<List<Note>> {
        return noteDao.getNotesFlow().map { list ->
            list.map { it.asExternalModel() }
        }
    }

    override fun getNoteWithCorpusFlow(): Flow<List<NoteWithCorpus>> {
        return noteDao.getNotesWithAggregate().map { list ->
            list.map {
                NoteWithCorpus(
                    note = it.note.asExternalModel(),
                    familiarCount = it.familiarCount,
                    unfamiliarCount = it.unfamiliarCount,
                    corpusCount = it.corpusCount
                )
            }
        }
    }

    override fun getNotesPagingFlow(): Flow<PagingData<NoteWithCorpus>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { noteDao.getPagedNotesWithAggregate() }
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            NoteWithCorpus(
                note = entity.note.asExternalModel(),
                familiarCount = entity.familiarCount,
                unfamiliarCount = entity.unfamiliarCount,
                corpusCount = entity.corpusCount
            )
        }
    }

    override fun getLastFiveCorpus(noteId: String): Flow<List<Corpus>> =
        noteDao.getLastFiveCorpusByNoteId(noteId).map { list ->
            list.map { it.asExternalModel() }
        }

    override fun getTrashNotesFlow(): Flow<PagingData<NoteWithCorpus>> = Pager(
        config = PagingConfig(pageSize = 10, enablePlaceholders = false),
        pagingSourceFactory = { noteDao.getTrashNotesFlow() }
    ).flow.map { pagingData ->
        pagingData.map {
            NoteWithCorpus(
                note = it.note.asExternalModel(),
                familiarCount = it.familiarCount,
                unfamiliarCount = it.unfamiliarCount,
                corpusCount = it.corpusCount
            )
        }
    }

    override suspend fun moveNotesToTrash(ids: List<String>) {
        noteDao.moveNotesToTrash(ids)
    }

    override suspend fun restoreNotesFromTrash(ids: List<String>) {
        noteDao.restoreNotesFromTrash(ids)
    }

    override suspend fun refreshSavedNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String,
        contentSize: Int
    ) {
        noteDao.refreshSavedNote(
            noteId,
            title,
            wordLang,
            meaningLang,
            contentSize
        )
    }

}

interface NoteRepository {
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun updateNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String
    ): Int

    suspend fun updateNoteSharedStatus(ids: List<String>, shared: Boolean)
    suspend fun incrementContentSize(id: String, count: Int)
    suspend fun decrementContentSize(id: String, count: Int)
    suspend fun deleteNotes(ids: List<String>)
    suspend fun getNoteById(id: String): Note
    fun getNotesFlow(): Flow<List<Note>>
    fun getNoteWithCorpusFlow(): Flow<List<NoteWithCorpus>>
    fun getNotesPagingFlow(): Flow<PagingData<NoteWithCorpus>>
    fun getLastFiveCorpus(noteId: String): Flow<List<Corpus>>
    fun getTrashNotesFlow(): Flow<PagingData<NoteWithCorpus>>
    suspend fun moveNotesToTrash(ids: List<String>)
    suspend fun restoreNotesFromTrash(ids: List<String>)
    suspend fun refreshSavedNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String,
        contentSize: Int,
    )
}

data class NoteWithCorpus(
    val note: Note,
    val familiarCount: Int,
    val unfamiliarCount: Int,
    val corpusCount: Int,
)