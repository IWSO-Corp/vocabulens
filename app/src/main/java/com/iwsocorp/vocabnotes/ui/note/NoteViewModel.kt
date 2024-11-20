package com.iwsocorp.vocabnotes.ui.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.Vocabulary
import kotlinx.coroutines.launch

class NoteViewModel : ViewModel() {

    private val _noteId = MutableLiveData<String>()
    val noteId: LiveData<String> get() = _noteId

    fun updateNoteId(newValue: String) {
        _noteId.value = newValue
    }

    fun searchWord(string: String, function: (vocabulary: Vocabulary?) -> Unit) {

    }

    fun getNote(noteId: String, function: (note: Note) -> Unit) = viewModelScope.launch {

    }

    fun createNewNote(note: Note) {}

}