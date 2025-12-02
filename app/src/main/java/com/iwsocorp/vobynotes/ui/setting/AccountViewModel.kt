package com.iwsocorp.vobynotes.ui.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.iwsocorp.vobynotes.core.data.repository.BackupRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.ShareRepository
import com.iwsocorp.vobynotes.core.model.SharedNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val shareRepository: ShareRepository,
    private val backupRepository: BackupRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val user = auth.currentUser!!

    private val _sharedNotes = MutableStateFlow<List<SharedNote>>(emptyList())
    val sharedNotes: MutableStateFlow<List<SharedNote>> = _sharedNotes

    fun getSharedNotes() = viewModelScope.launch {
        shareRepository.getUserSharedNotes(user.uid) {
            _sharedNotes.value = it
        }
        Timber.d("Get shared notes for user ${user.uid}")
    }

    fun changeName(newName: String, callback: (Boolean) -> Unit) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener {
            callback(it.isSuccessful)
        }
    }

    fun deleteAccountData(callback: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            backupRepository.deleteUserAccount(user.uid)
            shareRepository.deleteAccountSharedNotes(user.uid) {
                viewModelScope.launch {
                    noteRepository.updateNoteSharedStatus(it, false)
                }
            }
            callback(true)
        } catch (e: Exception) {
            callback(false)
            Timber.e(e)
        }
    }

}