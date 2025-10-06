package com.iwsocorp.vobynotes.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepository
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.core.model.Mark
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val exampleRepository: ExampleRepository,
) : ViewModel() {

    private val _noteId = MutableStateFlow<String?>(null)
    val noteId: StateFlow<String?> = _noteId

    fun setNoteId(noteId: String?) = viewModelScope.launch {
        _noteId.value = noteId
    }

    private val _mark = MutableStateFlow<Mark?>(null)
    val mark: StateFlow<Mark?> = _mark

    fun setMark(mark: Mark?) = viewModelScope.launch {
        _mark.value = mark
    }

    private val _wordLang = MutableStateFlow<String?>(null)
    val wordLang: StateFlow<String?> = _wordLang

    private val _meaningLang = MutableStateFlow<String?>(null)
    val meaningLang: StateFlow<String?> = _meaningLang

    private val _limit = MutableStateFlow(50)
    val limit: StateFlow<Int> = _limit

    fun setLimit(limit: Int) = viewModelScope.launch {
        _limit.value = limit
    }

    private val _quizList = MutableStateFlow<List<Example>>(emptyList())
    val quizList: StateFlow<List<Example>> = _quizList

    private val _isExampleEnough = MutableSharedFlow<Boolean>()
    val isExampleEnough: SharedFlow<Boolean> = _isExampleEnough

    fun getExamplesForQuiz(
        noteId: String? = null,
        mark: Mark? = null,
        wordLang: String? = null,
        meaningLang: String? = null,
        limit: Int = 50,
    ) = viewModelScope.launch {
        exampleRepository.getExamplesForQuiz(
            noteId,
            mark,
            wordLang,
            meaningLang,
            limit,
        ).collect {
            _quizList.value = it
            _isExampleEnough.emit(it.size >= limit)
        }
    }

}