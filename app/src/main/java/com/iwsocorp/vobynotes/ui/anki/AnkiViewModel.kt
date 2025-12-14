package com.iwsocorp.vobynotes.ui.anki

import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.AnkiRepository
import com.iwsocorp.vobynotes.core.data.repository.ExportResult
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AnkiViewModel @Inject constructor(
    private val ankiRepository: AnkiRepository
) : ViewModel() {

    private val _previewState = MutableSharedFlow<PreviewState>()
    val previewState = _previewState

    fun preview(note: Note, corpus: Corpus) = viewModelScope.launch(Dispatchers.IO) {
        _previewState.emit(PreviewState(loading = true))

        ankiRepository.previewFirstNote(note, corpus)
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

    fun checkDeckExists(note: Note): Boolean {
        return ankiRepository.checkDeckExists(note)
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
        corpusList: List<Corpus>,
        onProgress: (current: Int, total: Int) -> Unit
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

        // 3️⃣ Export (Background)
        performExport(corpusList, note, onProgress)
    }

    @SuppressLint("DirectSystemCurrentTimeMillisUsage")
    private fun performExport(
        corpusList: List<Corpus>,
        note: Note,
        onProgress: (Int, Int) -> Unit
    ) = viewModelScope.launch {
        _uiState.value = UiState.Loading

        val result = withContext(Dispatchers.IO) {
            runCatching {
                var totalAdded = 0
                val total = corpusList.size

                corpusList.forEachIndexed { index, corpus ->
                    val export = ankiRepository
                        .addCorpusToNoteDeck(note, corpus)
                        .getOrThrow()

                    totalAdded += when (export) {
                        is ExportResult.Success -> {
                            ankiRepository.markExported(
                                listOf(corpus.id),
                                System.currentTimeMillis()
                            )
                            export.added
                        }

                        else -> 0
                    }

                    // 🔔 Progress callback (UI / Notification)
                    onProgress(index + 1, total)
                }

                totalAdded
            }
        }

        result
            .onSuccess { added ->
                _uiState.value = UiState.Success(
                    noteTitle = note.title,
                    totalAdded = added
                )
            }
            .onFailure { e ->
                _uiState.value = UiState.Error(
                    e.message ?: "Failed to export note to Anki"
                )
            }
    }

    fun markUnexported(corpusIds: List<String>) = viewModelScope.launch {
        ankiRepository.markExported(corpusIds, null)
    }

    fun getNotExportedByNote(noteId: String, callback: (List<Corpus>) -> Unit) =
        viewModelScope.launch {
            ankiRepository.getNotExportedByNote(noteId).collect { callback(it) }
        }

    suspend fun handleBeforeSelect(note: Note) {
        if (!checkDeckExists(note)) {
            val list = ankiRepository.getExportedByNote(note.id)
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