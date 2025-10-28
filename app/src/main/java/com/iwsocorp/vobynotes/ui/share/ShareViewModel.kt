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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShareViewModel @Inject constructor(
    private val shareRepository: ShareRepository,
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
    private val exampleRepository: ExampleRepository,
) : ViewModel() {

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Loading)
    val shareState: StateFlow<ShareState> = _shareState

    val shareLink = "https://cerulean-hummingbird-a1ac12.netlify.app/sharedNote/"

    fun shareNote(sharedNote: SharedNote) = viewModelScope.launch {
        _shareState.value = ShareState.Loading
        try {
            val content = getNoteCorpus(sharedNote.id)
            shareRepository.shareNoteToPublic(sharedNote.copy(content = content))
            noteRepository.updateNoteSharedStatus(sharedNote.id, true)
            _shareState.value = ShareState.Shared("${shareLink}${sharedNote.id}")
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
            _shareState.value = ShareState.Shared("${shareLink}${noteId}")
        } catch (e: Exception) {
            _shareState.value = ShareState.Error(e.message ?: "Unknown error")
        }
    }

    fun getSharedNotes(
        filterWordLang: String? = null,
        filterMeaningLang: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: Query.Direction = Query.Direction.DESCENDING
    ): Flow<PagingData<SharedNote>> {
        _shareState.value = ShareState.Loading
        return shareRepository.getPagedSharedNotes(
            filterWordLang,
            filterMeaningLang,
            sortBy,
            sortDirection
        ).cachedIn(viewModelScope)
    }

    fun setLoaded() {
        _shareState.value = ShareState.Loaded
    }

}

sealed class ShareState {
    object Loading : ShareState()
    object Loaded : ShareState()
    data class Shared(val link: String) : ShareState()
    data class Error(val message: String) : ShareState()
}