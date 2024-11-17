package com.iwsocorp.vocabnotes.ui.note

import androidx.lifecycle.ViewModel
import com.iwsocorp.vocabnotes.data.model.Note
import com.iwsocorp.vocabnotes.data.model.Vocabulary

class NoteViewModel : ViewModel() {

    fun searchWord(string: String, function: (vocabulary: Vocabulary?) -> Unit) {

    }

    fun getNote(string: String, function: (note: Note) -> Unit) {}

}