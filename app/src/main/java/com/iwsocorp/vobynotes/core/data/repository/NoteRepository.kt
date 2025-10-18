package com.iwsocorp.vobynotes.core.data.repository

import com.iwsocorp.vobynotes.core.database.dao.NoteDao
import com.iwsocorp.vobynotes.core.database.dao.NoteWithLatestCorpus
import com.iwsocorp.vobynotes.core.database.model.asExternalModel
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    override fun getNotesWithCorpusFlow(): Flow<List<NoteWithCorpus>> = combine(
        noteDao.getNotesWithCorpusCountFlow(),
        noteDao.getAllCorpusFlow()
    ) { notes, _ ->
        notes.map { partial ->
            NoteWithLatestCorpus(
                partial.note,
                partial.corpusCount,
                noteDao.countNoteContentMark(partial.note.id, Mark.FAMILIAR),
                noteDao.countNoteContentMark(partial.note.id, Mark.UNFAMILIAR),
                noteDao.getLastFiveCorpusByNoteIdSuspend(partial.note.id)
            )
        }
    }.map { list ->
        list.map {
            NoteWithCorpus(
                note = it.note.asExternalModel(),
                familiarCount = it.familiarCount,
                unfamiliarCount = it.unfamiliarCount,
                corpus = it.corpus.map { entity -> entity.asExternalModel() }
            )
        }
    }.map { list ->
        listOf(
            NoteWithCorpus(
                note = Note(
                    id = "",
                    title = "All Vocabulary",
                    wordLang = "",
                    meaningLang = "",
                    contentSize = 0,
                    createdAt = 0L,
                    updatedAt = 0L
                ),
                familiarCount = 0,
                unfamiliarCount = 0,
                corpus = emptyList()
            )
        ) + list
    }

    override fun getTrashNotesFlow(): Flow<List<NoteWithCorpus>> = combine(
        noteDao.getTrashNotesFlow(),
        noteDao.getAllTrashCorpusFlow()
    ) { notes, _ ->
        notes.map { partial ->
            NoteWithLatestCorpus(
                partial.note,
                partial.corpusCount,
                noteDao.countNoteContentMark(partial.note.id, Mark.FAMILIAR),
                noteDao.countNoteContentMark(partial.note.id, Mark.UNFAMILIAR),
                noteDao.getLastFiveCorpusByNoteIdSuspend(partial.note.id)
            )
        }
    }.map { list ->
        list.map {
            NoteWithCorpus(
                note = it.note.asExternalModel(),
                familiarCount = it.familiarCount,
                unfamiliarCount = it.unfamiliarCount,
                corpus = it.corpus.map { entity -> entity.asExternalModel() }
            )
        }
    }

    override suspend fun moveNotesToTrash(ids: List<String>) {
        noteDao.moveNotesToTrash(ids)
    }

    override suspend fun restoreNotesFromTrash(ids: List<String>) {
        noteDao.restoreNotesFromTrash(ids)
    }

}

interface NoteRepository {
    suspend fun addNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun incrementContentSize(id: String, count: Int)
    suspend fun decrementContentSize(id: String, count: Int)
    suspend fun deleteNotes(ids: List<String>)
    suspend fun getNoteById(id: String): Note
    fun getNotesFlow(): Flow<List<Note>>
    fun getNotesWithCorpusFlow(): Flow<List<NoteWithCorpus>>
    fun getTrashNotesFlow(): Flow<List<NoteWithCorpus>>
    suspend fun moveNotesToTrash(ids: List<String>)
    suspend fun restoreNotesFromTrash(ids: List<String>)
}

data class NoteWithCorpus(
    val note: Note,
    val familiarCount: Int,
    val unfamiliarCount: Int,
    val corpus: List<Corpus>,
)