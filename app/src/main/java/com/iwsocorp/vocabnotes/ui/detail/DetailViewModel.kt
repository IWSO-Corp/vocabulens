package com.iwsocorp.vocabnotes.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.MeaningRepository
import com.iwsocorp.vocabnotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Meaning
import com.iwsocorp.vocabnotes.core.model.Vocabulary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val corpusRepository: CorpusRepository,
    private val meaningRepository: MeaningRepository,
    private val vocabularyRepository: VocabularyRepository,
) : ViewModel() {

    fun getVocabulary(word: String, callback: (vocabulary: Vocabulary) -> Unit) =
        viewModelScope.launch {
            callback(vocabularyRepository.getVocabulary(word))
        }

    fun getCorpusByWord(word: String, callback: (corpus: Corpus) -> Unit) = viewModelScope.launch {
        callback(corpusRepository.getCorpusByWord(word))
    }

    fun updateCorpus(corpus: Corpus) = viewModelScope.launch {
        corpusRepository.updateCorpus(corpus)
    }

    fun insertWordMeanings(word: String, meanings: List<Meaning>) = viewModelScope.launch {
        meaningRepository.insertMeanings(word = word, meanings = meanings)
    }

}