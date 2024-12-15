package com.iwsocorp.vocabnotes.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    fun getAllNotes(): Flow<PagingData<Note>> = noteRepository.getNotes().cachedIn(viewModelScope)

    fun allCorpusSize(): Flow<Int> = corpusRepository.allCorpusSize()

    fun getCorpusByNoteId(noteId: String, callback: (List<Corpus>) -> Unit) =
        viewModelScope.launch {
            corpusRepository.getLatestCorpus(noteId).collect {
                callback(it)
            }
        }

    fun deleteNote(noteId: String) = viewModelScope.launch {
        noteRepository.deleteNote(noteId)
        corpusRepository.deleteCorpusByNoteId(noteId)
    }
}