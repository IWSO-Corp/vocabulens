package com.iwsocorp.vocabnotes.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.ExampleRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Example
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.Vocabulary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
    private val exampleRepository: ExampleRepository,
    private val vocabularyRepository: VocabularyRepository,
) : ViewModel() {

    fun getNote(id: String, callback: (note: Note) -> Unit) = viewModelScope.launch {
        callback(noteRepository.getNoteById(id))
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        noteRepository.updateNote(note)
    }

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