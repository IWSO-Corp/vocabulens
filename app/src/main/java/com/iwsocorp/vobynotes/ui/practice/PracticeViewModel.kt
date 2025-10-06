package com.iwsocorp.vobynotes.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.ExampleRepository
import com.iwsocorp.vobynotes.core.model.Example
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val exampleRepository: ExampleRepository,
) : ViewModel() {

    private val _currentExample = MutableStateFlow<Example?>(null)
    val currentExample: StateFlow<Example?> = _currentExample

    private val _questionSentence = MutableStateFlow("")
    val questionSentence: StateFlow<String> = _questionSentence

    private val _options = MutableStateFlow<List<String>>(emptyList())
    val options: StateFlow<List<String>> = _options

    fun loadNewQuestion() = viewModelScope.launch {
        val allExamples = exampleRepository.getAll()
        val example = allExamples.random()
        _currentExample.value = example

        // Hilangkan kata target dari kalimat
        val sentenceWithBlank = example.sentence.replace(
            Regex("\\b${example.forWord}\\b", RegexOption.IGNORE_CASE),
            "_____"
        )
        _questionSentence.value = sentenceWithBlank

        // Buat opsi acak (1 benar + 3 salah)
        val wrongOptions = allExamples.map { it.forWord }
            .filterNot { it == example.forWord }
            .shuffled()
            .take(3)

        _options.value = (wrongOptions + example.forWord).shuffled()
    }

}