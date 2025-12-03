package com.iwsocorp.vobynotes.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.iwsocorp.vobynotes.core.common.CorpusQueryStateDataStore
import com.iwsocorp.vobynotes.core.common.Utils.withIndex
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.CorpusWithNote
import com.iwsocorp.vobynotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.toCorpus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val corpusRepository: CorpusRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val dataStore: CorpusQueryStateDataStore
) : ViewModel() {

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: MutableStateFlow<SearchUiState> = _searchUiState

    fun setIdleState() {
        _searchUiState.value = SearchUiState.Idle
    }

    fun searchWord(query: String) = viewModelScope.launch {
        _searchUiState.value = SearchUiState.Loading

        corpusRepository.searchCorpus(query)
            .map { pagingData ->
                pagingData.withIndex().map { indexed ->
                    val newCorpus = indexed.value.corpus.copy(indexNumber = indexed.index)
                    SearchUiModel.Item(
                        data = indexed.value.copy(corpus = newCorpus)
                    )
                }
            }
            .map { pagingData ->
                pagingData.insertSeparators { before, after ->
                    val beforeNoteId = before?.data?.corpus?.noteId
                    val afterItem = after?.data

                    if (afterItem != null && beforeNoteId != afterItem.corpus.noteId) {
                        SearchUiModel.Header(
                            noteId = afterItem.corpus.noteId,
                            noteTitle = afterItem.noteTitle
                        )
                    } else null
                }
            }
            .cachedIn(viewModelScope)
            .collectLatest {
                _searchUiState.value = SearchUiState.LocalLoaded(it)
            }
    }

    fun searchWordDefinition(word: String, meaning: String) = viewModelScope.launch {
        _searchUiState.value = SearchUiState.Loading

        dataStore.translationLang.filterNotNull().collectLatest { lang ->
            _searchUiState.value = SearchUiState.ApiLoaded(
                vocabularyRepository.getVocabulary(word).toCorpus(lang, meaning)
            )
        }
    }

    fun updateCorpusMark(corpusIds: List<String>, newMark: Mark) = viewModelScope.launch {
        corpusRepository.updateCorpusMark(corpusIds, newMark)
    }

    suspend fun getCorpusByWord(word: String): Corpus? = corpusRepository.getCorpusByWord(word)

}

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class LocalLoaded(val corpusPagingData: PagingData<SearchUiModel>) : SearchUiState()
    data class ApiLoaded(val corpus: Corpus) : SearchUiState()
}

sealed class SearchUiModel {
    data class Header(val noteId: String, val noteTitle: String) : SearchUiModel()
    data class Item(val data: CorpusWithNote) : SearchUiModel()
}