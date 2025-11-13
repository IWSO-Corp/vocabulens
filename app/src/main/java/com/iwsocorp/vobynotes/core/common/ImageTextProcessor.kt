package com.iwsocorp.vobynotes.core.common

import android.content.Context
import android.net.Uri
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.iwsocorp.vobynotes.core.common.Utils.removePunctuation
import com.iwsocorp.vobynotes.core.model.WordResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageTextProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: CorpusQueryStateDataStore
) {

    private val identifier = LanguageIdentification.getClient()
    private val translatorCache = mutableMapOf<String, Translator>()

    suspend fun processImage(uri: Uri, sourceLang: String?, targetLang: String): List<WordResult> {
        val textBlocks = detectText(uri, sourceLang)
        if (textBlocks.isEmpty()) return emptyList()
        return identifyAndTranslate(textBlocks, sourceLang, targetLang)
            .sortedBy { it.word }
            .distinct()
    }

    fun isModelDownloaded(langCode: String, onResult: (Boolean) -> Unit) {
        val modelManager = RemoteModelManager.getInstance()
        val model = TranslateRemoteModel.Builder(langCode).build()

        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { isDownloaded ->
                Timber.d("Model $langCode downloaded: $isDownloaded")
                onResult(isDownloaded)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    private suspend fun detectText(uri: Uri, sourceLang: String?): List<String> {
        val recognizer = getTextRecognizerByLanguage(sourceLang ?: "")
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizer.process(image).await()
        return result.textBlocks.flatMap { block ->
            block.lines.map { it.text }
        }
    }

    fun getTextRecognizerByLanguage(langCode: String): TextRecognizer = TextRecognition.getClient(
        when (langCode) {
            "zh" -> ChineseTextRecognizerOptions.Builder().build()
            "ja" -> JapaneseTextRecognizerOptions.Builder().build()
            "ko" -> KoreanTextRecognizerOptions.Builder().build()
            "hi" -> DevanagariTextRecognizerOptions.Builder().build()
            else -> TextRecognizerOptions.DEFAULT_OPTIONS
        }
    )

    private suspend fun identifyAndTranslate(
        sentences: List<String>,
        sourceLang: String?,
        targetLang: String
    ): List<WordResult> {
        val allWords = sentences.flatMap { line ->
            line.removePunctuation()
                .split(" ")
                .filter { it.isNotBlank() && !it.all(Char::isDigit) }
        }.distinct()

        return allWords.map { word ->
            val detectedLang = identifier.identifyLanguage(word).await()
            val translator = getTranslator(
                source = sourceLang ?: detectedLang,
                target = targetLang
            )
            val translated = translator.translate(word).await()

            WordResult(
                if (word.trim().uppercase() == "I") word.trim().uppercase()
                else word.trim().lowercase(),
                detectedLang,
                translated.trim().lowercase(),
                targetLang
            )
        }
    }

    private suspend fun getTranslator(source: String, target: String): Translator {
        val key = "$source-$target"

        val translator = translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(source)
                .setTargetLanguage(target)
                .build()
            Translation.getClient(options)
        }

        // pastikan model sudah diunduh
        translator.downloadModelIfNeeded().await()

        return translator
    }

    suspend fun translateSingle(word: String): String {
        val translator = getTranslator(
            TranslateLanguage.ENGLISH,
            dataStore.translationLang.filterNotNull().first()
        )
        return translator.translate(word).await()
    }

    suspend fun identifyLanguage(word: String): String {
        return identifier.identifyLanguage(word).await()
    }

    fun deleteTempFile(uri: Uri) {
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