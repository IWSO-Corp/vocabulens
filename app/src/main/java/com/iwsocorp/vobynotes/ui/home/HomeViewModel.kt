package com.iwsocorp.vobynotes.ui.home

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iwsocorp.vobynotes.core.data.repository.CorpusRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteRepository
import com.iwsocorp.vobynotes.core.data.repository.NoteWithCorpus
import com.iwsocorp.vobynotes.core.database.dao.InsertResult
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val corpusRepository: CorpusRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> get() = _uiState

    fun getNotesWithCorpusFlow() {
        _uiState.value = UiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.getNotesWithCorpusFlow().collect {
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Loaded(it)
                }
            }
        }
    }

    private val _allCorpus = MutableStateFlow<List<Corpus>>(emptyList())
    val allCorpus: StateFlow<List<Corpus>> get() = _allCorpus

    fun allCorpus() = viewModelScope.launch(Dispatchers.IO) {
        corpusRepository.allCorpusFlow().collect {
            withContext(Dispatchers.Main) {
                _allCorpus.value = it
            }
        }
    }

    fun moveNotesToTrash(noteIds: List<String>) = viewModelScope.launch {
        noteRepository.moveNotesToTrash(noteIds)
    }

    fun readExcelFile(inputStream: InputStream): List<Corpus> {
        val corpusList = mutableListOf<Corpus>()

        try {
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            for (row in sheet) {
                val worldLang = row.getCell(0).stringCellValue
                val meaningLang = row.getCell(1).stringCellValue
                val word = row.getCell(2).stringCellValue
                val meaning = row.getCell(3).stringCellValue

                val corpus = Corpus(
                    noteId = "",
                    word = word,
                    meaning = meaning,
                    wordLang = worldLang,
                    meaningLang = meaningLang,
                )

                corpusList.add(corpus)
            }

            workbook.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return corpusList
    }

    fun getFileName(contentResolver: ContentResolver, uri: Uri): String? {
        var fileName: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                // Retrieve the file name from the OpenableColumns.DISPLAY_NAME column
                fileName = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return fileName
    }

    fun importCorpusBatch(
        fileName: String?,
        corpusBatch: List<Corpus>,
        existingCount: (existingCount: Int) -> Unit,
        insertResult: (result: InsertResult) -> Unit,
    ) = viewModelScope.launch {
        if (corpusBatch.isEmpty()) return@launch

        val words = corpusBatch.map { it.word }
        val wordLang = corpusBatch.first().wordLang
        val meaningLang = corpusBatch.first().meaningLang

        // Hitung corpus yang sudah ada di DB
        val existingCount = corpusRepository.countExisting(words)
        existingCount(existingCount)
        if (existingCount == words.size) return@launch

        // Buat Note baru
        val note = Note(
            title = fileName ?: "$wordLang-$meaningLang",
            wordLang = wordLang,
            meaningLang = meaningLang,
            contentSize = corpusBatch.size,
        )
        noteRepository.addNote(note)

        // Insert corpus baru (yang belum ada)
        val corpusWithNote = corpusBatch.map { it.copy(noteId = note.id) }
        insertResult(corpusRepository.insertCorpusList(corpusWithNote))
    }

    init {
        getNotesWithCorpusFlow()
        allCorpus()
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Loaded(val notes: List<NoteWithCorpus>) : UiState()
}