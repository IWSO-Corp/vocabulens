package com.iwsocorp.vobynotes.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.Query
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.ShareRepository
import com.iwsocorp.vobynotes.core.model.SharedCorpus
import com.iwsocorp.vobynotes.core.model.SharedNote
import com.iwsocorp.vobynotes.core.model.asSharedCorpus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

const val shareLink = "https://cerulean-hummingbird-a1ac12.netlify.app/shared-note/"

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val shareRepository: ShareRepository,
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
    private val exampleRepository: ExampleRepository,
) : ViewModel() {

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState

    fun shareNote(sharedNote: SharedNote) = viewModelScope.launch {
        _shareState.value = ShareState.Loading
        try {
            val content = getNoteCorpus(sharedNote.id)
            shareRepository.shareNoteToPublic(sharedNote.copy(content = content))
            noteRepository.updateNoteSharedStatus(sharedNote.id, true)
            _shareState.value = ShareState.Shared(sharedNote.title, "${shareLink}${sharedNote.id}")
        } catch (e: Exception) {
            _shareState.value = ShareState.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun getNoteCorpus(noteId: String): List<SharedCorpus> =
        corpusRepository.getCorpusListByNoteId(noteId).map { corpus ->
            val examples = exampleRepository.getExamplesForWord(corpus.word).map { it.sentence }
            corpus.asSharedCorpus(examples)
        }

    fun updateSharedNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String,
    ) = viewModelScope.launch {
        _shareState.value = ShareState.Loading
        try {
            val content = getNoteCorpus(noteId)
            shareRepository.updateSharedNote(noteId, title, wordLang, meaningLang, content)
            _shareState.value = ShareState.Shared(title, "${shareLink}${noteId}")
        } catch (e: Exception) {
            _shareState.value = ShareState.Error(e.message ?: "Unknown error")
        }
    }

    fun getSharedNotes(
        filterWordLang: String? = null,
        filterMeaningLang: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: Query.Direction = Query.Direction.DESCENDING
    ) = viewModelScope.launch {
        _shareState.value = ShareState.Loading
        shareRepository.getPagedSharedNotes(
            filterWordLang,
            filterMeaningLang,
            sortBy,
            sortDirection
        ).cachedIn(viewModelScope).collectLatest {
            _shareState.value = ShareState.Loaded(it)
        }
    }

    var filter = ""
    var sort = ""

    fun updateFilter(filterData: String) {
        filter = filterData
    }

    fun updateSort(sortData: String) {
        sort = sortData
    }

    var recyclerPosition = 0

    fun updateRecyclerPosition(position: Int) {
        recyclerPosition = position
    }

}

sealed class ShareState {
    object Idle : ShareState()
    object Loading : ShareState()
    data class Loaded(val notes: PagingData<SharedNote>) : ShareState()
    data class Shared(val noteTitle: String, val link: String) : ShareState()
    data class Error(val message: String) : ShareState()
}