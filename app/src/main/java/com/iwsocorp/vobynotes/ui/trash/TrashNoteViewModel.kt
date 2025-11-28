package com.iwsocorp.vobynotes.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import com.iwsocorp.vobynotes.core.model.SortBy
import com.iwsocorp.vobynotes.core.model.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    fun getNote(noteId: String, callback: (note: Note) -> Unit) = viewModelScope.launch {
        callback(noteRepository.getNoteById(noteId))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPagedCorpus(noteId: String): Flow<PagingData<Corpus>> = corpusRepository.getPagedCorpus(
        noteId,
        null,
        SortBy.WORD,
        SortOrder.ASC,
    ).map { pagingData ->
        var counter = 0
        pagingData.map { entity ->
            counter++
            entity.copy(indexNumber = counter)
        }
    }.cachedIn(viewModelScope)

    fun deleteNotes(ids: List<String>) = viewModelScope.launch {
        noteRepository.deleteNotes(ids)
    }

    fun restoreNotes(ids: List<String>) = viewModelScope.launch {
        noteRepository.restoreNotesFromTrash(ids)
    }

}