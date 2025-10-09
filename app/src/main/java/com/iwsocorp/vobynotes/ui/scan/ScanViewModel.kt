package com.iwsocorp.vobynotes.ui.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState

    fun setIdle() {
        _uiState.value = ScanUiState.Idle
    }

    fun setError(message: String) {
        _uiState.value = ScanUiState.Error(message)
    }

    fun processImage(uri: Uri) = viewModelScope.launch {
        _uiState.value = ScanUiState.Loading

        try {
            val textBlocks: List<String> = detectText(uri)
            val results: List<WordResult> = identifyAndTranslate(textBlocks)

            _uiState.value = ScanUiState.Success(
                imageUri = uri,
                results = results
            )

            deleteTempFile(uri)
        } catch (e: Exception) {
            _uiState.value = ScanUiState.Error(
                "Error occurred: ${e.message}" ?: "Error occurred during processing"
            )
        }
    }

    private suspend fun detectText(uri: Uri): List<String> {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        return result.textBlocks.flatMap { it.lines.map { line -> line.text } }
    }

    private suspend fun identifyAndTranslate(texts: List<String>): List<WordResult> {
        val identifier = LanguageIdentification.getClient()
        val translatorMap = mutableMapOf<String, Translator>()

        return texts.flatMap { line ->
            line.split(" ").filter { it.isNotBlank() }.map { word ->
                val language = identifier.identifyLanguage(word).await()

                val translator = translatorMap.getOrPut(TranslateLanguage.ENGLISH) {
                    getTranslator(TranslateLanguage.ENGLISH, TranslateLanguage.INDONESIAN)
                }

                val translated = translator.translate(word).await()
                WordResult(word, language, translated)
            }
        }
    }

    private suspend fun getTranslator(source: String, target: String): Translator {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)

        _uiState.value = ScanUiState.DownloadingModel(source, target)

        try {
            translator.downloadModelIfNeeded().await()
        } catch (e: Exception) {
            _uiState.value = ScanUiState.Error("Failed to download model: ${e.message}")
            throw e
        }

        return translator
    }

    private fun deleteTempFile(uri: Uri) {
        try {
            val file = File(uri.path ?: return)
            if (file.exists()) {
                val deleted = file.delete()
                Timber.d("Temp file deleted: $deleted - ${file.name}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete temp file")
        }
    }

}

data class WordResult(
    val word: String,
    val language: String,
    val translation: String,
)

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Loading : ScanUiState()
    data class Success(
        val imageUri: Uri,
        val results: List<WordResult>,
    ) : ScanUiState()

    data class DownloadingModel(val sourceLang: String, val targetLang: String) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}