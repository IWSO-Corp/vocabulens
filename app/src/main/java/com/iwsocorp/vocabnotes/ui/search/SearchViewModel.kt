package com.iwsocorp.vocabnotes.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Mark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _searchResults = MutableLiveData<PagingData<Corpus>>()
    val searchResults: LiveData<PagingData<Corpus>> = _searchResults

    fun searchWord(word: String) = viewModelScope.launch {
        corpusRepository.searchCorpus(word).collect {
            _searchResults.value = it
        }
    }

    fun updateCorpusMark(corpusWords: List<String>, newMark: Mark) = viewModelScope.launch {
        corpusRepository.updateCorpusMark(corpusWords, newMark)
    }

}