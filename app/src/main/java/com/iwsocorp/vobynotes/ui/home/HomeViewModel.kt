package com.iwsocorp.vobynotes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteWithCorpus
import com.iwsocorp.vobynotes.core.model.Corpus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> get() = _uiState

    fun getNotesWithCorpusFlow() {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.getNotesWithCorpusFlow().collect {
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Loaded(it)
                }
            }
        }
    }

    private val _allCorpus = MutableStateFlow<List<Corpus>>(emptyList())
    val allCorpus: StateFlow<List<Corpus>> get() = _allCorpus

    fun allCorpus() = viewModelScope.launch(Dispatchers.IO) {
        corpusRepository.allCorpusFlow().collect {
            withContext(Dispatchers.Main) {
                _allCorpus.value = it
            }
        }
    }

    fun deleteNote(noteId: String) = viewModelScope.launch {
        noteRepository.deleteNote(noteId)
    }

    init {
        getNotesWithCorpusFlow()
        allCorpus()
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Loaded(val notes: List<NoteWithCorpus>) : UiState()
}