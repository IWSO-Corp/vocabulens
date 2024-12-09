package com.iwsocorp.vocabnotes.ui.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.flatMap
import androidx.paging.map
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _noteId = MutableLiveData<String>()
    val noteId: LiveData<String> get() = _noteId

    fun updateNoteId(newValue: String) {
        _noteId.value = newValue
    }

    fun getNote(noteId: String, callback: (note: Note) -> Unit) = viewModelScope.launch {
        val note = noteRepository.getNoteById(noteId)
        callback(note)
    }

    fun createNewNote(note: Note) = viewModelScope.launch {
        noteRepository.addNote(note)
    }

    fun getCorpusPagingDataFlow(noteId: String): Flow<PagingData<Corpus>> =
        corpusRepository.getCorpusByNoteId(noteId).cachedIn(viewModelScope)

    fun insertCorpus(corpus: Corpus) = viewModelScope.launch {
        corpusRepository.addCorpus(corpus)
    }

    fun insertCorpusList(corpusList: List<Corpus>) = viewModelScope.launch {
        corpusRepository.insertCorpusList(corpusList)
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

}