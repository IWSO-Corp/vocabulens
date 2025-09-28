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
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Mark
import com.iwsocorp.vocabnotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> get() = _notes

    fun getNotes() = viewModelScope.launch {
        _notes.value = noteRepository.getNoteList()
    }

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> get() = _note
    private val _noteId = MutableLiveData<String?>()
    val noteId: LiveData<String?> get() = _noteId
    private val _noteTitle = MutableLiveData<String>()
    val noteTitle: LiveData<String> get() = _noteTitle

    fun updateNoteId(newValue: String?) {
        _noteId.value = newValue
    }

    fun updateNoteTitle(newValue: String) {
        _noteTitle.value = newValue
    }

    fun getNote(noteId: String) = viewModelScope.launch {
        _note.value = noteRepository.getNoteById(noteId)
    }

    fun getCorpusPagingDataFlow(noteId: String): Flow<PagingData<Corpus>> =
        corpusRepository.getCorpusByNoteId(noteId).cachedIn(viewModelScope)

    fun insertCorpus(corpus: Corpus, callback: (result: Long) -> Unit) = viewModelScope.launch {
        val existingCorpus = corpusRepository.getCorpusByWord(corpus.word)
        Timber.d("existingCorpus: $existingCorpus")
        if (existingCorpus != null) {
            callback(-1L)
            return@launch
        } else {
            Timber.d("noteId.value: ${noteId.value}")
            if (noteId.value == null) {
                val newNote = Note(
                    title = noteTitle.value ?: "Untitled",
                    wordLang = corpus.wordLang,
                    meaningLang = corpus.meaningLang,
                    contentSize = 1,
                )
                noteRepository.addNote(newNote)
                _noteId.value = newNote.id

                val corpusNew = corpus.copy(noteId = newNote.id)
                callback(corpusRepository.addCorpus(corpusNew))
            } else {
                noteRepository.incrementContentSize(noteId.value!!, 1)
                callback(corpusRepository.addCorpus(corpus))
            }
        }
    }

    suspend fun importCorpusBatch(
        fileName: String?,
        corpusBatch: List<Corpus>,
        existingCount: (existingCount: Int) -> Unit,
        insertResult: (result: InsertResult) -> Unit,
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
            title = fileName ?: "$wordLang-$meaningLang",
            wordLang = wordLang,
            meaningLang = meaningLang,
            contentSize = corpusBatch.size,
        )
        noteRepository.addNote(note)

        // Insert corpus baru (yang belum ada)
        val corpusWithNote = corpusBatch.map { it.copy(noteId = note.id) }
        insertResult(corpusRepository.insertCorpusList(corpusWithNote))
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }

    fun getAllCorpus(): Flow<PagingData<Corpus>> = corpusRepository.getAllCorpus()

    fun deleteNote(noteId: String) = viewModelScope.launch {
        noteRepository.deleteNote(noteId)
    }

    fun deleteCorpusBatch(words: List<String>) = viewModelScope.launch {
        corpusRepository.deleteBatch(words)
        noteRepository.decrementContentSize(noteId.value!!, words.size)
    }

    fun moveCorpusToNote(corpusWords: List<String>, newNoteId: String) = viewModelScope.launch {
        corpusRepository.moveCorpusToNote(corpusWords, newNoteId)
        noteRepository.decrementContentSize(noteId.value!!, corpusWords.size)
        noteRepository.incrementContentSize(newNoteId, corpusWords.size)
    }

    fun updateCorpusMark(corpusWords: List<String>, newMark: Mark) = viewModelScope.launch {
        corpusRepository.updateCorpusMark(corpusWords, newMark)
    }

    init {
        getNotes()
    }

}