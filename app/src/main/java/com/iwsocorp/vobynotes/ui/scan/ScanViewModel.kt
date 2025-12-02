package com.iwsocorp.vobynotes.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.common.CorpusQueryStateDataStore
import com.iwsocorp.vobynotes.core.common.ImageTextProcessor
import com.iwsocorp.vobynotes.core.model.WordResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val processor: ImageTextProcessor,
    private val dataStore: CorpusQueryStateDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState

    fun setIdle() {
        _uiState.value = ScanUiState.Idle
    }

    fun setError(message: String) {
        _uiState.value = ScanUiState.Error(message)
    }

    fun processImage(uri: Uri, sourceLang: String?, targetLang: String) = viewModelScope.launch {
        _uiState.value = ScanUiState.Loading
        try {
            processor.isModelDownloaded(sourceLang ?: "en") {
                if (!it) {
                    _uiState.value =
                        ScanUiState.DownloadingModel(sourceLang ?: "Unknown", targetLang)
                }
            }
            processor.isModelDownloaded(targetLang) {
                if (!it) {
                    _uiState.value =
                        ScanUiState.DownloadingModel(sourceLang ?: "Unknown", targetLang)
                }
            }

            val results = processor.processImage(uri, sourceLang, targetLang)
            if (results.isEmpty()) {
                _uiState.value = ScanUiState.Error("No text found in the image")
                return@launch
            }

            _uiState.value = ScanUiState.Success(uri, results)
            processor.deleteTempFile(uri)
        } catch (e: Exception) {
            _uiState.value = ScanUiState.Error("Error occurred: ${e.message}")
        }
    }

    fun translate(word: String, result: (String) -> Unit) = viewModelScope.launch {
        try {
            val translated = processor.translateSingle(word)
            result(translated)
        } catch (e: Exception) {
            _uiState.value = ScanUiState.Error("Failed to translate: ${e.message}")
        }
    }

    val isInfoShowed: StateFlow<Boolean?> = dataStore.isInfoShowed.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        false
    )

    fun getLatestImage(context: Context): Uri? = processor.getLatestImage(context)

}

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class DownloadingModel(val sourceLang: String, val targetLang: String) : ScanUiState()
    data class Success(val imageUri: Uri, val results: List<WordResult>) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}