package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.model.Corpus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImportViewModel : ViewModel() {

    private val _importedCorpus = MutableStateFlow<List<Corpus>>(emptyList())
    val importedCorpus: StateFlow<List<Corpus>> = _importedCorpus.asStateFlow()

    fun setImportedData(data: List<Corpus>) = viewModelScope.launch {
        _importedCorpus.value = data
    }

}