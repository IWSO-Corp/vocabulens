package com.iwsocorp.vobynotes.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.ui.home.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> get() = _uiState

    fun getTrashNotes() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            noteRepository.getTrashNotesFlow().cachedIn(viewModelScope).collectLatest {
                _uiState.value = UiState.Loaded(it)
            }
        }
    }

    fun getLastFiveCorpus(noteId: String, callback: (List<Corpus>) -> Unit) = viewModelScope.launch {
        noteRepository.getLastFiveCorpus(noteId).collectLatest {
            callback(it)
        }
    }

    fun moveNotesToTrash(ids: List<String>) = viewModelScope.launch {
        noteRepository.moveNotesToTrash(ids)
    }

    fun deleteNotes(ids: List<String>) = viewModelScope.launch {
        noteRepository.deleteNotes(ids)
    }

    fun restoreNotes(ids: List<String>) = viewModelScope.launch {
        noteRepository.restoreNotesFromTrash(ids)
    }

    init {
        getTrashNotes()
    }

}
