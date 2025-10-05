package com.iwsocorp.vobynotes.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepository
import com.iwsocorp.vobynotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.core.model.Mark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val corpusRepository: CorpusRepository,
    private val exampleRepository: ExampleRepository,
    private val vocabularyRepository: VocabularyRepository,
) : ViewModel() {

    private val _corpusId = MutableLiveData<String>()
    val corpusId: LiveData<String> get() = _corpusId

    fun setCorpusId(word: String) {
        _corpusId.value = word
    }

    private val _corpusPosition = MutableLiveData<Int>()
    val corpusPosition: LiveData<Int> get() = _corpusPosition

    fun setCorpusPosition(position: Int) {
        _corpusPosition.value = position
    }

    private val _corpusList = MutableLiveData<List<Corpus>>()
    val corpusList: LiveData<List<Corpus>> get() = _corpusList

    fun setCorpusList(wordList: List<Corpus>) {
        _corpusList.value = wordList
    }

    val posAndCorpusList: LiveData<Pair<Int, List<Corpus>>> = corpusPosition.asFlow()
        .combine(corpusList.asFlow()) { pos, list ->
            Timber.d("Position: $pos, List corpus: ${list.size}")
            Pair(pos, list)
        }.asLiveData()

    private val _corpus = MutableLiveData<Corpus?>()
    val corpus: LiveData<Corpus?> get() = _corpus

    fun getCorpusById(id: String) = viewModelScope.launch {
        corpusRepository.getCorpusById(id).distinctUntilChanged().collect {
            _corpus.value = it
        }
    }

    fun resetCorpus() {
        _corpus.value = null
    }

    fun updateCorpusDetail(word: String) = viewModelScope.launch {
        _corpus.value?.let {
            val vocab = vocabularyRepository.getVocabulary(word)
            val newCorpus = it.copy(
                phonetic = vocab.phonetic,
                audio = vocab.audio,
                meanings = vocab.meanings,
                updatedAt = System.currentTimeMillis()
            )
            corpusRepository.updateCorpus(newCorpus)
        }
    }

    fun updateCorpus(currentWord: String, corpus: Corpus, callback: (result: Long) -> Unit) =
        viewModelScope.launch {
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

    @OptIn(FlowPreview::class)
    fun getExamplesByWord(word: String, allCorpusFlow: Flow<List<Corpus>>): Flow<List<Example>> =
        exampleRepository.getExamplesByWord(word).debounce(200).distinctUntilChanged()
            .combine(
                allCorpusFlow.debounce(200).distinctUntilChanged()
            ) { exampleList, corpusList ->
                val examples = mutableListOf<Example>().apply { addAll(exampleList) }

                corpusList.forEach { corpus ->
                    corpus.meanings.forEach { meaning ->
                        meaning.definitions.forEach { definition ->
                            definition.example?.let { sentence ->
                                if (containsWordRegex(sentence, word)) {
                                    examples.add(Example(word, sentence))
                                }
                                if (word == corpus.word && sentence.isNotEmpty()) {
                                    examples.add(Example(word, sentence))
                                }
                            }
                        }
                    }
                }

                Timber.d("Examples flow [$word]: ${examples.map { it.sentence }}")
                examples.distinctBy { it.sentence }.shuffled().take(5)
            }
            .distinctUntilChanged() // <- cegah emit berulang untuk data sama

    fun containsWordRegex(sentence: String, word: String): Boolean {
        val pattern = "\\b${Regex.escape(word)}\\b".toRegex(RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(sentence)
    }

    fun updateCorpusMark(corpusIds: List<String>, newMark: Mark) = viewModelScope.launch {
        corpusRepository.updateCorpusMark(corpusIds, newMark)
    }

    fun deleteCorpus(corpusIds: List<String>) = viewModelScope.launch {
        corpusRepository.deleteBatch(corpusIds)
    }

}