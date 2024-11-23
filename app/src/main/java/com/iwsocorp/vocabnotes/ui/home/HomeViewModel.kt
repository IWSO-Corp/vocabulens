package com.iwsocorp.vocabnotes.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    fun getAllNotes() = viewModelScope.launch {
        noteRepository.getNotes().collectLatest {
            _notes.value = it
        }
    }

    init {
        getAllNotes()
    }

}