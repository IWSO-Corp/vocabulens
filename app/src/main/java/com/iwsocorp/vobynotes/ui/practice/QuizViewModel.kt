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

    fun startQuiz(examples: List<Example>) {
        if (examples.isNotEmpty()) {
            _quizState.value = QuizState.ShowQuestion(
                examples[currentIndex],
                currentIndex + 1,
                examples.size
            )
        } else {
            _quizState.value = QuizState.Finished
        }
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
                examples.size
            )
        } else {
            _quizState.value = QuizState.Finished
        }
    }
}

sealed class QuizState {
    data class ShowQuestion(
        val example: Example,
        val position: Int,
        val total: Int,
    ) : QuizState()

    data class ShowResult(
        val isCorrect: Boolean,
        val correctAnswer: String,
        val selectedAnswer: String,
        val sentence: String,
    ) : QuizState()

    object Idle : QuizState()
    object Finished : QuizState()
}