package com.iwsocorp.vobynotes.ui.anki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.AnkiRepository
import com.iwsocorp.vobynotes.core.database.dao.NoteExportStat
import com.iwsocorp.vobynotes.core.model.Corpus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnkiViewModel @Inject constructor(
    private val ankiRepository: AnkiRepository
) : ViewModel() {

    val isAnkiAvailable: Boolean = ankiRepository.isAnkiAvailable()

    fun getNoteExportStats(): Flow<List<NoteExportStat>> = ankiRepository.getNoteExportStats()

    private val _previewState = MutableSharedFlow<PreviewState>()
    val previewState = _previewState

    fun preview(title: String, wordLang: String, meaningLang: String, corpus: Corpus) =
        viewModelScope.launch(Dispatchers.IO) {
            _previewState.emit(PreviewState(loading = true))

            ankiRepository.previewFirstNote(title, wordLang, meaningLang, corpus)
                .onSuccess {
                    _previewState.emit(PreviewState(cards = it))
                }
                .onFailure {
                    _previewState.emit(PreviewState(error = it.message ?: "Preview failed"))
                }
        }

    fun setPreviewState(state: PreviewState) = viewModelScope.launch {
        _previewState.emit(state)
    }

    fun checkDeckExists(title: String, wordLang: String, meaningLang: String): Boolean {
        return ankiRepository.checkDeckExists(title, wordLang, meaningLang)
    }

    fun markUnexported(corpusIds: List<String>) = viewModelScope.launch {
        ankiRepository.markUnexported(corpusIds)
    }

    fun getNotExportedByNote(noteId: String): Flow<List<Corpus>> = ankiRepository.getNotExportedByNote(noteId)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun sendListToAnki(
        title: String, wordLang: String, meaningLang: String,
        corpora: List<Corpus>,
        onProgress: (Int, Int) -> Unit
    ) = viewModelScope.launch {
        _uiState.value = UiState.Loading

        val result =
            ankiRepository.sendListToAnki(title, wordLang, meaningLang, corpora, onProgress)

        result.onSuccess { (success, failed) ->
            _uiState.value = UiState.Success(title, success, failed)
        }.onFailure {
            _uiState.value = UiState.Error(
                it.message ?: "Failed to export note to Anki"
            )
        }
    }

    suspend fun handleBeforeSelect(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String
    ) {
        if (!checkDeckExists(title, wordLang, meaningLang)) {
            val list = ankiRepository.getExportedByNote(noteId)
            markUnexported(list.map { it.id })
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun mapPreviewToUi(preview: Map<String, Map<String, String>>): List<PreviewCardUi> {
        return preview.map { (cardName, html) ->
            PreviewCardUi(
                cardName = cardName,
                frontHtml = wrapAnkiHtml(html["q"].orEmpty()),
                backHtml = wrapAnkiHtml(html["a"].orEmpty())
            )
        }
    }

    fun wrapAnkiHtml(rawHtml: String): String {

        val css = ANKI_LIGHT_CSS

        return """
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <style>$css</style>
        </head>
        <body>
            $rawHtml
        </body>
        </html>
    """.trimIndent()
    }

}

data class PreviewState(
    val loading: Boolean = false,
    val cards: Map<String, Map<String, String>>? = null,
    val error: String? = null
)

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(
        val noteTitle: String,
        val success: Int,
        val failed: Int
    ) : UiState

    data class Error(
        val message: String
    ) : UiState
}

data class PreviewCardUi(
    val cardName: String,
    val frontHtml: String,
    val backHtml: String
)

private const val ANKI_LIGHT_CSS = """
body {
    background-color: #ffffff;
    color: #000000;
    font-size: 20px;
    padding: 16px;
    text-align: center;
}
"""