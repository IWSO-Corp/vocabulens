package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    fun backupToFirestore(userId: String) {
        viewModelScope.launch {
            _backupState.value = BackupState.Loading
            try {
                backupRepository.backupToFirestore(userId)
                _backupState.value = BackupState.Success
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun restoreFromFirestoreAndInsertToDatabase(userId: String) = viewModelScope.launch {
        _backupState.value = BackupState.Loading
        try {
            backupRepository.restoreFromFirestoreAndInsertToDatabase(userId)
            _backupState.value = BackupState.Restored
        } catch (e: Exception) {
            _backupState.value = BackupState.Error(e.message ?: "Unknown error")
        }
    }

}

sealed class BackupState {
    object Idle : BackupState()
    object Loading : BackupState()
    object Success : BackupState()
    object Restored : BackupState()
    data class Error(val message: String) : BackupState()
}