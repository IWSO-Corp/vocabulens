package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val translationLanguage: StateFlow<String?> = dataStore.translationLang.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        ""
    )

    fun setTranslationLanguage(lang: String) = viewModelScope.launch {
        dataStore.setTranslationLang(lang)
    }

}