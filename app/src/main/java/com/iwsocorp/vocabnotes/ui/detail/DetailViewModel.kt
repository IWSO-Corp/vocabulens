package com.iwsocorp.vocabnotes.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.ExampleRepository
import com.iwsocorp.vocabnotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Example
import com.iwsocorp.vocabnotes.core.model.toCorpus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val corpusRepository: CorpusRepository,
    private val exampleRepository: ExampleRepository,
    private val vocabularyRepository: VocabularyRepository,
) : ViewModel() {

    private val _corpusWord = MutableLiveData<String>()
    val corpusWord: LiveData<String> get() = _corpusWord

    fun setCorpusWord(word: String) {
        _corpusWord.value = word
    }

    private val _corpusPosition = MutableLiveData<Int>()
    val corpusPosition: LiveData<Int> get() = _corpusPosition

    fun setCorpusPosition(position: Int) {
        _corpusPosition.value = position
    }

    private val _wordList = MutableLiveData<List<String>>()
    val wordList: LiveData<List<String>> get() = _wordList

    fun setCorpusList(wordList: List<String>) {
        _wordList.value = wordList
    }

    val posAndWords: LiveData<Pair<Int, List<String>>> = corpusPosition.asFlow()
        .combine(wordList.asFlow()) { pos, list ->
            Timber.d("Position: $pos, List: $list")
            Pair(pos, list)
        }.asLiveData()

    private val _corpus = MutableLiveData<Corpus?>()
    val corpus: LiveData<Corpus?> get() = _corpus

    fun getCorpus(word: String) = viewModelScope.launch {
        _corpus.value = corpusRepository.getCorpusByWord(word)
    }

    fun resetCorpus() {
        _corpus.value = null
    }

    fun searchWordDefinition(word: String) = viewModelScope.launch {
        _corpus.value = vocabularyRepository.getVocabulary(word).toCorpus()
    }

    fun updateCorpusDetail(word: String) = viewModelScope.launch(Dispatchers.IO) {
        val corpus = _corpus.value ?: return@launch
        val vocab = vocabularyRepository.getVocabulary(word)
        val newCorpus = corpus.copy(
            phonetic = vocab.phonetic,
            audio = vocab.audio,
            meanings = vocab.meanings,
            updatedAt = System.currentTimeMillis()
        )
        withContext(Dispatchers.Main) {
            _corpus.value = newCorpus
        }
        corpusRepository.updateCorpus(newCorpus)
    }

    suspend fun getCorpusByWord(word: String): Corpus? = corpusRepository.getCorpusByWord(word)

    fun updateCorpus(currentWord: String, corpus: Corpus, callback: (result: Long) -> Unit) = viewModelScope.launch {
        Timber.d("Updating corpus: $corpus")
        if (currentWord != corpus.word) {
            val existingCorpus = corpusRepository.getCorpusByWord(corpus.word)
            if (existingCorpus != null) {
                callback(-1L)
                return@launch
            } else {
                corpusRepository.updateCorpus(corpus)
                _corpus.value = corpus
                callback(1L)
            }
        } else {
            corpusRepository.updateCorpus(corpus)
            _corpus.value = corpus
            callback(1L)
        }
    }

    fun insertExampleSentence(example: Example) = viewModelScope.launch {
        exampleRepository.insertExampleSentence(example)
    }

    fun getExamplesByWord(word: String, callback: (examples: List<Example>) -> Unit) =
        viewModelScope.launch {
            exampleRepository.getExamplesByWord(word).collect {
                callback(it)
            }
        }

}