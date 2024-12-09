package com.iwsocorp.vocabnotes.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.flatMap
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.apache.commons.collections4.CollectionUtils.collect
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    fun getAllNotes() = viewModelScope.launch {
        noteRepository.getNotes().collect {
            _notes.value = it
        }
    }

    fun getCorpusByNoteId(noteId: String, callback: (List<Corpus>) -> Unit) =
        viewModelScope.launch {
            corpusRepository.getLatestCorpus(noteId).collect {
                callback(it)
            }
        }

}