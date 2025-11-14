package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.BackupRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteWithCorpus
import com.iwsocorp.vobynotes.core.database.dao.InsertResult
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import com.iwsocorp.vobynotes.core.database.model.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
    private val noteRepository: NoteRepository,
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    fun setIdle() {
        _backupState.value = BackupState.Idle
    }

    fun backupToFirestore(userId: String) = viewModelScope.launch {
        _backupState.value = BackupState.Loading
        try {
            backupRepository.backupToFirestore(userId)
            _backupState.value = BackupState.Success
        } catch (e: Exception) {
            _backupState.value = BackupState.Error(e.message ?: "Unknown error")
        }
    }

    fun insertToDatabase(backupData: BackupData, result: (InsertResult) -> Unit) =
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            try {
                backupRepository.insertToDatabase(backupData, result)
                _backupState.value = BackupState.Restored
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Unknown error")
            }
        }

    private val _backupData = MutableStateFlow<BackupData?>(null)
    val backupData: StateFlow<BackupData?> = _backupData

    fun resetBackupData() {
        _backupData.value = null
    }

    fun getBackupData(userId: String) = viewModelScope.launch {
        try {
            val notes = backupRepository.restoreAllNotes(userId)
            val corpus = backupRepository.restoreAllCorpus(userId)
            val examples = backupRepository.restoreAllExamples(userId)
            Timber.d("Backup data vm: notes=${notes.size}, corpus=${corpus.size}, examples=${examples.size}")
            _backupData.value = BackupData(notes, corpus, examples)
        } catch (e: Exception) {
            _backupData.value = null
            Timber.e(e)
        }
    }

    private val _localData = MutableStateFlow<BackupData?>(null)
    val localData: StateFlow<BackupData?> = _localData

    fun getLocalData() = viewModelScope.launch {
        backupRepository.getLocalData().collectLatest {
            _localData.value = it
        }
    }

    private val _notes = MutableLiveData<List<NoteWithCorpus>>()
    val notes: LiveData<List<NoteWithCorpus>> get() = _notes

    fun getNotes() = viewModelScope.launch {
        noteRepository.getNoteWithCorpusFlow().collectLatest {
            _notes.value = it
        }
    }

    init {
        getLocalData()
        getNotes()
    }

}

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    object Success : BackupState()
    object Restored : BackupState()
    data class Error(val message: String) : BackupState()
}

data class BackupData(
    val notes: List<NoteEntity>,
    val corpus: List<CorpusEntity>,
    val examples: List<ExampleEntity>
)