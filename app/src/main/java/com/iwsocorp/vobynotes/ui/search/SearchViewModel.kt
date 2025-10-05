package com.iwsocorp.vobynotes.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _searchResults = MutableLiveData<PagingData<Corpus>>()
    val searchResults: LiveData<PagingData<Corpus>> = _searchResults

    fun searchWord(word: String) = viewModelScope.launch {
        corpusRepository.searchCorpus(word)
            .map { pagingData ->
                var counter = 0
                pagingData.map { entity ->
                    counter++
                    entity.copy(indexNumber = counter)
                }
            }.collect {
                _searchResults.value = it
            }
    }

    fun updateCorpusMark(corpusIds: List<String>, newMark: Mark) = viewModelScope.launch {
        corpusRepository.updateCorpusMark(corpusIds, newMark)
    }

    suspend fun getCorpusByWord(word: String): Corpus? = corpusRepository.getCorpusByWord(word)

}