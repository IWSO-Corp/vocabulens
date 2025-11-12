package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.iwsocorp.vobynotes.core.common.CorpusQueryStateDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val dataStore: CorpusQueryStateDataStore,
) : ViewModel() {

    val appLanguage: StateFlow<String?> = dataStore.appLang.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        ""
    )

    fun setAppLanguage(lang: String) = viewModelScope.launch {
        dataStore.setAppLang(lang)
    }

    val sourceLanguage: StateFlow<String?> = dataStore.sourceLang.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        ""
    )

    fun setSourceLanguage(lang: String) = viewModelScope.launch {
        dataStore.setSourceLang(lang)
    }

    val translationLanguage: StateFlow<String?> = dataStore.translationLang.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        ""
    )

    fun setTranslationLanguage(lang: String) = viewModelScope.launch {
        dataStore.setTranslationLang(lang)
    }

    fun switchLanguage() = viewModelScope.launch {
        dataStore.switchLanguage()
    }

    fun setInfoShowed(isInfoShowed: Boolean?) = viewModelScope.launch {
        dataStore.setInfoShowed(isInfoShowed)
    }

    private val modelManager = RemoteModelManager.getInstance()

    fun checkDownloadedModels(onResult: (List<String>) -> Unit) =
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val modelNames = models.map { it.language }
                onResult(modelNames)
            }
            .addOnFailureListener { e ->
                onResult(listOf("Error: ${e.message}"))
            }

    fun deleteModel(langCode: String, onResult: (Boolean) -> Unit) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener { onResult(true) }
            .addOnFailureListener { onResult(false) }
    }

}