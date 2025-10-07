package com.iwsocorp.vobynotes.ui.practice

import androidx.lifecycle.ViewModel
import com.iwsocorp.vobynotes.core.model.Example
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuizViewModel : ViewModel() {

    private val _quizState = MutableStateFlow<QuizState>(QuizState.Idle)
    val state: StateFlow<QuizState> = _quizState.asStateFlow()
    private var currentIndex = 0
    private val _result = mutableListOf<Triple<String, Boolean, String>>()

    fun startQuiz(examples: List<Example>) {
        _quizState.value = QuizState.ShowQuestion(
            examples[currentIndex],
            currentIndex + 1,
        )
    }

    fun answerQuestion(selected: String, examples: List<Example>) {
        val currentExample = examples[currentIndex]
        val isCorrect = selected == currentExample.forWord

        _quizState.value = QuizState.ShowResult(
            isCorrect = isCorrect,
            correctAnswer = currentExample.forWord,
            selectedAnswer = selected,
            sentence = currentExample.sentence
        )
    }

    fun nextQuestion(examples: List<Example>) {
        currentIndex++
        if (currentIndex < examples.size) {
            _quizState.value = QuizState.ShowQuestion(
                examples[currentIndex],
                currentIndex + 1,
            )
        } else {
            _quizState.value = QuizState.Finished(_result)
        }
    }

    fun saveResult(sentence: String, isCorrect: Boolean, selectedAnswer: String) = _result.add(
        Triple(sentence, isCorrect, selectedAnswer)
    )
}

sealed class QuizState {
    object Idle : QuizState()
    data class ShowQuestion(
        val example: Example,
        val position: Int,
    ) : QuizState()

    data class ShowResult(
        val isCorrect: Boolean,
        val correctAnswer: String,
        val selectedAnswer: String,
        val sentence: String,
    ) : QuizState()

    data class Finished(
        val result: List<Triple<String, Boolean, String>>,
    ) : QuizState()
}