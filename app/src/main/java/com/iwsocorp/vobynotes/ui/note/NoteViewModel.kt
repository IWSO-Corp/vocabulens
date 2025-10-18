package com.iwsocorp.vobynotes.ui.note

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.iwsocorp.vobynotes.core.common.CorpusQueryStateDataStore
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.database.dao.InsertResult
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.SortBy
import com.iwsocorp.vobynotes.core.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
    private val queryStore: CorpusQueryStateDataStore,
) : ViewModel() {

    val queryState: StateFlow<CorpusQueryState> = queryStore.queryState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CorpusQueryState())

    fun setFilter(mark: Mark?) = viewModelScope.launch {
        queryStore.saveQueryState(queryState.value.copy(mark))
    }

    fun setSort(sortBy: SortBy, sort: SortOrder) = viewModelScope.launch {
        queryStore.saveQueryState(
            queryState.value.copy(sortBy = sortBy, sortOrder = sort)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPagedCorpus(noteId: String): Flow<PagingData<Corpus>> =
        queryState.flatMapLatest { state ->
            corpusRepository.getPagedCorpus(
                noteId,
                state.mark,
                state.sortBy,
                state.sortOrder
            ).map { pagingData ->
                var counter = 0
                pagingData.map { entity ->
                    counter++
                    entity.copy(indexNumber = counter)
                }
            }
        }.cachedIn(viewModelScope)

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

    fun createNote(note: Note) = viewModelScope.launch {
        noteRepository.addNote(note)
    }

    fun insertCorpusList(
        corpusList: List<Corpus>,
        insertResult: (result: InsertResult) -> Unit,
    ) = viewModelScope.launch {
        insertResult(corpusRepository.insertCorpusList(corpusList))
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }

    fun moveNotesToTrash(noteIds: List<String>) = viewModelScope.launch {
        noteRepository.moveNotesToTrash(noteIds)
    }

    fun deleteCorpusBatch(ids: List<String>) = viewModelScope.launch {
        corpusRepository.deleteBatch(ids)
        noteRepository.decrementContentSize(noteId.value!!, ids.size)
    }

    fun moveCorpusToNote(corpusIds: List<String>, newNoteId: String) = viewModelScope.launch {
        corpusRepository.moveCorpusToNote(corpusIds, newNoteId)
        noteRepository.decrementContentSize(noteId.value!!, corpusIds.size)
        noteRepository.incrementContentSize(newNoteId, corpusIds.size)
    }

    fun updateCorpusMark(corpusIds: List<String>, newMark: Mark) = viewModelScope.launch {
        corpusRepository.updateCorpusMark(corpusIds, newMark)
    }

    companion object {
        const val QUERY_KEY = "query_state"
    }

    init {
        getNotes()
    }

}

@Parcelize
data class CorpusQueryState(
    val mark: Mark? = null,
    val sortBy: SortBy = SortBy.WORD,
    val sortOrder: SortOrder = SortOrder.ASC,
) : Parcelable