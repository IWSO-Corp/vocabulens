package com.iwsocorp.vocabnotes.ui.note

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vocabnotes.core.data.repository.CorpusRepository
import com.iwsocorp.vocabnotes.core.data.repository.MeaningRepository
import com.iwsocorp.vocabnotes.core.data.repository.NoteRepository
import com.iwsocorp.vocabnotes.core.data.repository.VocabularyRepository
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.Meaning
import com.iwsocorp.vocabnotes.core.model.Note
import com.iwsocorp.vocabnotes.core.model.Vocabulary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
    private val meaningRepository: MeaningRepository,
    private val vocabularyRepository: VocabularyRepository,
) : ViewModel() {

    private val _noteId = MutableLiveData<String>()
    val noteId: LiveData<String> get() = _noteId

    fun updateNoteId(newValue: String) {
        _noteId.value = newValue
    }

    fun getNote(noteId: String, callback: (note: Note) -> Unit) = viewModelScope.launch {
        val note = noteRepository.getNoteById(noteId)
        callback(note)
    }

    fun createNewNote(note: Note) = viewModelScope.launch {
        noteRepository.addNote(note)
    }

    suspend fun getVocabulary(word: String): Vocabulary {
        return vocabularyRepository.getVocabulary(word)
    }

    fun insertCorpus(corpus: Corpus) = viewModelScope.launch {
        corpusRepository.addCorpus(corpus)
    }

    fun insertWordMeanings(wordId: String, meanings: List<Meaning>) = viewModelScope.launch {
        meaningRepository.insertMeanings(wordId = wordId, meanings = meanings)
    }

    private val _corpusList = MutableLiveData<List<Corpus>>()
    val corpusList: LiveData<List<Corpus>> get() = _corpusList

    fun getCorpusByNoteId(noteId: String) = viewModelScope.launch {
        corpusRepository.getCorpusByNoteId(noteId).collectLatest {
            _corpusList.value = it
        }
    }

}