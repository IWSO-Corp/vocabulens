package com.iwsocorp.vobynotes.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    fun getAllNotes(): Flow<PagingData<Note>> = noteRepository.getNotes().cachedIn(viewModelScope)

    private val _allCorpus = MutableStateFlow<List<Corpus>>(emptyList())
    val allCorpus = _allCorpus

    fun allCorpus() = viewModelScope.launch(Dispatchers.IO) {
        corpusRepository.allCorpus().collect {
            withContext(Dispatchers.Main) {
                _allCorpus.value = it
            }
        }
    }

    fun getCorpusByNoteId(noteId: String, callback: (List<Corpus>) -> Unit) =
        viewModelScope.launch {
            corpusRepository.getLatestCorpus(noteId).collect {
                callback(it)
            }
        }

    fun deleteNote(noteId: String) = viewModelScope.launch {
        noteRepository.deleteNote(noteId)
    }

    fun countMark(noteId: String, mark: Mark): Flow<Int> = flow {
        emit(corpusRepository.countMark(noteId, mark))
    }

    init {
        allCorpus()
    }
}