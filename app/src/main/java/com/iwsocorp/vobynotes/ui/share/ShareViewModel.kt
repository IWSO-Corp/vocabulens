package com.iwsocorp.vobynotes.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.firebase.firestore.Query
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepository
import com.iwsocorp.vobynotes.core.data.repository.ShareRepository
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
    private val corpusRepository: CorpusRepository,
    private val exampleRepository: ExampleRepository,
) : ViewModel() {

    private val _shareState = MutableStateFlow<ShareState>(ShareState.Idle)
    val shareState: StateFlow<ShareState> = _shareState

    val shareLink = "https://cerulean-hummingbird-a1ac12.netlify.app/sharedNote/"

    fun shareNote(sharedNote: SharedNote) = viewModelScope.launch {
        _shareState.value = ShareState.Loading
        try {
            val content = corpusRepository.getCorpusListByNoteId(sharedNote.id).map { corpus ->
                val examples = exampleRepository.getExamplesForWord(corpus.word).map { it.sentence }
                corpus.asSharedCorpus(examples)
            }
            shareRepository.shareNoteToPublic(sharedNote.copy(content = content))
            _shareState.value = ShareState.Shared("${shareLink}${sharedNote.id}")
        } catch (e: Exception) {
            _shareState.value = ShareState.Error(e.message ?: "Unknown error")
        }
    }

    fun getSharedNotes(
        filterWordLang: String? = null,
        filterMeaningLang: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: Query.Direction = Query.Direction.DESCENDING
    ): Flow<PagingData<SharedNote>> = shareRepository.getPagedSharedNotes(
        filterWordLang,
        filterMeaningLang,
        sortBy,
        sortDirection
    ).cachedIn(viewModelScope)

}

sealed class ShareState {
    object Idle : ShareState()
    object Loading : ShareState()
    data class Shared(val link: String) : ShareState()
    data class Loaded(val sharedNotes: PagingData<SharedNote>) : ShareState()
    data class Error(val message: String) : ShareState()
}