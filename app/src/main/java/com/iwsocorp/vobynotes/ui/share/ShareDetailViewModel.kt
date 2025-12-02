package com.iwsocorp.vobynotes.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.BackupRepository
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.ShareRepository
import com.iwsocorp.vobynotes.core.database.dao.InsertResult
import com.iwsocorp.vobynotes.core.model.SharedNote
import com.iwsocorp.vobynotes.core.model.asCorpus
import com.iwsocorp.vobynotes.core.model.asEntity
import com.iwsocorp.vobynotes.core.model.asNote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ShareDetailViewModel @Inject constructor(
    private val shareRepository: ShareRepository,
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _sharedNoteState = MutableStateFlow<SharedNoteState>(SharedNoteState.Loading)
    val sharedNoteState: StateFlow<SharedNoteState> = _sharedNoteState

    fun getSharedNote(noteId: String) = viewModelScope.launch {
        _sharedNoteState.value = SharedNoteState.Loading
        _sharedNoteState.value = shareRepository.getSharedNoteById(noteId)?.let {
            SharedNoteState.Loaded(it)
        } ?: SharedNoteState.Error("Note not found")
    }

    private val _saveState = MutableSharedFlow<String>()
    val saveState: SharedFlow<String> = _saveState

    fun saveSharedNote(
        userId: String,
        sharedNote: SharedNote,
        existingCount: (existingCount: Int) -> Unit,
        insertResult: (result: InsertResult) -> Unit,
    ) = shareRepository.saveSharedNoteTransaction(userId, sharedNote.id) { result ->
        viewModelScope.launch {
            result.onSuccess {
                val existCount = corpusRepository.countExisting(sharedNote.content.map { it.word })
                existingCount(existCount)
                if (existCount == sharedNote.content.size) return@launch

                noteRepository.addNote(sharedNote.asNote())
                val result =
                    backupRepository.insertCorpusSafely(sharedNote.content.map { it.asCorpus() }
                        .map { it.asEntity() })
                insertResult(result)

                _saveState.emit("Saved")
            }.onFailure {
                Timber.e(it)
                _saveState.emit("Failed")
            }
        }
    }

    fun refreshSavedNote(
        sharedNote: SharedNote,
        existingCount: (existingCount: Int) -> Unit,
        insertResult: (result: InsertResult) -> Unit,
    ) = viewModelScope.launch {
        val existCount = corpusRepository.countExisting(sharedNote.content.map { it.word })
        existingCount(existCount)

        if (existCount == sharedNote.content.size) {
            _saveState.emit("Note already up to date")
            return@launch
        }

        noteRepository.refreshSavedNote(
            sharedNote.id,
            sharedNote.title,
            sharedNote.wordLang,
            sharedNote.meaningLang,
            sharedNote.content.size,
        )

        val result = backupRepository.insertCorpusSafely(sharedNote.content.map { it.asCorpus() }
            .map { it.asEntity() })
        insertResult(result)

        _saveState.emit(if (result.successCount > 0) "Updated" else "Note already up to date")
    }

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    fun setIsSaved(isSaved: Boolean) {
        _isSaved.value = isSaved
    }

    fun checkNoteSavedStatus(userId: String, noteId: String, callback: (Boolean) -> Unit) =
        viewModelScope.launch {
            callback(shareRepository.checkNoteSavedStatus(userId, noteId))
        }

    fun deleteSharedNote(noteId: String, callback: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            shareRepository.deleteSharedNote(noteId)
            noteRepository.updateNoteSharedStatus(listOf(noteId), false)
            callback(true)
        } catch (e: Exception) {
            callback(false)
            Timber.e(e)
        }
    }

}

sealed class SharedNoteState {
    object Loading : SharedNoteState()
    data class Loaded(val sharedNote: SharedNote) : SharedNoteState()
    data class Error(val message: String) : SharedNoteState()
}