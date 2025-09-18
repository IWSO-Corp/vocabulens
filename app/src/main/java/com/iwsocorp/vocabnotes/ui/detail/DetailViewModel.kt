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

    private val _updatedCorpus = MutableLiveData<Corpus?>()
    val updatedCorpus: LiveData<Corpus?> get() = _updatedCorpus

    fun resetUpdatedCorpus() {
        _updatedCorpus.value = null
    }

    fun updateCorpusDetail(
        corpusWord: String,
        isNetworkAvailable: Boolean,
        showToast: () -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        val corpus = corpusRepository.getCorpusByWord(corpusWord)
        if (corpus.phonetic.isEmpty() && isNetworkAvailable) {
            val vocab = vocabularyRepository.getVocabulary(corpusWord)
            val newCorpus = Corpus(
                word = corpus.word,
                noteId = corpus.noteId,
                meaning = corpus.meaning,
                wordLang = corpus.wordLang,
                meaningLang = corpus.meaningLang,
                phonetic = vocab.phonetic,
                audio = vocab.audio,
                meanings = vocab.meanings,
                createdAt = corpus.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            withContext(Dispatchers.Main) {
                _updatedCorpus.value = newCorpus
            }
            corpusRepository.updateCorpus(newCorpus)
        } else if (!isNetworkAvailable) {
            withContext(Dispatchers.Main) {
                showToast()
                _updatedCorpus.value = corpus
            }
        } else {
            withContext(Dispatchers.Main) {
                _updatedCorpus.value = corpus
            }
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