package com.iwsocorp.vobynotes.ui.anki

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.AnkiRepository
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnkiViewModel @Inject constructor(
    private val ankiRepository: AnkiRepository
) : ViewModel() {

    private val _previewState = MutableStateFlow(PreviewState())
    val previewState = _previewState.asStateFlow()

    fun preview(note: Note, corpus: Corpus) = viewModelScope.launch(Dispatchers.IO) {
        _previewState.value = PreviewState(loading = true)

        ankiRepository.previewFirstNote(note, corpus)
            .onSuccess {
                _previewState.value = PreviewState(cards = it)
            }
            .onFailure {
                _previewState.value = PreviewState(
                    error = it.message ?: "Preview failed"
                )
            }
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Export seluruh Corpus dalam satu Note
     * 1 Note = 1 Deck Anki
     */
    fun exportNoteToAnki(
        activity: Activity,
        note: Note,
        corpusList: List<Corpus>
    ) {
        // 1️⃣ Validasi Anki
        if (!ankiRepository.isAnkiAvailable()) {
            _uiState.value = UiState.AnkiNotInstalled
            return
        }

        // 2️⃣ Permission
        if (ankiRepository.shouldRequestPermission()) {
            ankiRepository.requestPermission(activity)
            _uiState.value = UiState.PermissionRequired
            return
        }

        // 3️⃣ Export
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                _uiState.value = UiState.Loading

                var totalAdded = 0

                corpusList.forEach { corpus ->
                    totalAdded += ankiRepository.addCorpusToNoteDeck(corpus, note).getOrThrow()
                }

                totalAdded
            }.onSuccess { added ->
                ankiRepository.markExported(corpusList.map { it.id })
                _uiState.value = UiState.Success(
                    noteTitle = note.title,
                    totalAdded = added
                )
            }.onFailure { e ->
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to export note to Anki"
                )
            }
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
        val totalAdded: Int
    ) : UiState

    object AnkiNotInstalled : UiState
    object PermissionRequired : UiState

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
}
.card {
    text-align: center;
}
"""