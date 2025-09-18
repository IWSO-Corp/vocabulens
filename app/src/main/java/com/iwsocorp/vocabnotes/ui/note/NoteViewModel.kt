package com.iwsocorp.vocabnotes.ui.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.database.dao.InsertResult
import com.iwsocorp.vocabnotes.core.database.model.NoteEntity
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.asEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _noteId = MutableLiveData<String?>()
    val noteId: LiveData<String?> get() = _noteId

    fun updateNoteId(newValue: String?) {
        _noteId.value = newValue
    }

    fun getNote(noteId: String, callback: (note: Note) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val note = noteRepository.getNoteById(noteId)
        callback(note)
    }

    fun createNewNote(note: Note) = viewModelScope.launch {
        noteRepository.addNote(note)
    }

    fun getCorpusPagingDataFlow(noteId: String): Flow<PagingData<Corpus>> =
        corpusRepository.getCorpusByNoteId(noteId).cachedIn(viewModelScope)

    fun insertCorpus(corpus: Corpus, callback: (result: Long) -> Unit) = viewModelScope.launch {
        callback(corpusRepository.addCorpus(corpus))
    }

    fun insertCorpusList(corpusList: List<Corpus>, callback: (result: InsertResult) -> Unit) = viewModelScope.launch {
        callback(corpusRepository.insertCorpusList(corpusList))
    }

    suspend fun importCorpusBatch(
        corpusBatch: List<Corpus>,
        existingCount: (existingCount: Int) -> Unit,
        insertResult: (result: InsertResult) -> Unit
    ) {
        if (corpusBatch.isEmpty()) return

        val words = corpusBatch.map { it.word }
        val wordLang = corpusBatch.first().wordLang
        val meaningLang = corpusBatch.first().meaningLang

        // Hitung corpus yang sudah ada di DB
        val existingCount = corpusRepository.countExisting(words)
        existingCount(existingCount)
        if (existingCount == words.size) return

        // Buat Note baru
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = "$wordLang-$meaningLang",
            wordLang = wordLang,
            meaningLang = meaningLang,
            contentSize = corpusBatch.size,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteRepository.addNote(note)

        // Insert corpus baru (yang belum ada)
        val corpusWithNote = corpusBatch.map { it.copy(noteId = note.id) }
        insertResult(corpusRepository.insertCorpusList(corpusWithNote))
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }

    fun updateNoteUpdatedAt(id: String, updatedAt: Long) = viewModelScope.launch {
        noteRepository.updateUpdatedAt(id, updatedAt)
    }

    fun updateNoteContentSize(id: String, contentSize: Int) = viewModelScope.launch {
        noteRepository.updateContentSize(id, contentSize)
    }

    fun getAllCorpus(): Flow<PagingData<Corpus>> = corpusRepository.getAllCorpus()

    fun deleteNote(noteId: String) = viewModelScope.launch {
        noteRepository.deleteNote(noteId)
    }
}