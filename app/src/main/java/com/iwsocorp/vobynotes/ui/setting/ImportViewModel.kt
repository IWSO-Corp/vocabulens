package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _importedCorpus = MutableStateFlow<List<Corpus>>(emptyList())
    val importedCorpus: StateFlow<List<Corpus>> = _importedCorpus.asStateFlow()

    fun setImportedData(data: List<Corpus>) = viewModelScope.launch {
        _importedCorpus.value = data
    }

    fun updateNote(noteId: String, title: String, wordLang: String, meaningLang: String, result: (Int) -> Unit) =
        viewModelScope.launch {
            result(noteRepository.updateNote(noteId, title, wordLang, meaningLang))
        }

}