package com.iwsocorp.vobynotes.core.data.repository

import android.content.Context
import com.iwsocorp.vobynotes.core.data.anki.AnkiDroidConfig
import com.iwsocorp.vobynotes.core.data.anki.AnkiDroidHelper
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper.generateTags
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper.toAnkiFields
import com.iwsocorp.vobynotes.core.database.dao.CorpusDao
import com.iwsocorp.vobynotes.core.database.dao.NoteDao
import com.iwsocorp.vobynotes.core.database.dao.NoteExportStat
import com.iwsocorp.vobynotes.core.database.model.asExternalModel
import com.iwsocorp.vobynotes.core.model.Corpus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class AnkiRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val noteDao: NoteDao,
    private val corpusDao: CorpusDao,
) : AnkiRepository {

    private val helper = AnkiDroidHelper(context)

    override fun isAnkiAvailable(): Boolean {
        return AnkiDroidHelper.isApiAvailable(helper.context)
    }

    override fun checkDeckExists(title: String, wordLang: String, meaningLang: String): Boolean {
        return helper.checkDeckExists(title, wordLang, meaningLang)
    }

    override suspend fun previewFirstNote(
        title: String, wordLang: String, meaningLang: String,
        corpus: Corpus
    ): Result<Map<String, Map<String, String>>> = runCatching {
        val deckId = helper.getOrCreateDeckIdForNote(title, wordLang, meaningLang)
        Timber.d("Deck ID: $deckId")

        val modelId = getOrCreateModelId(deckId)
        Timber.d("Model ID: $modelId")

        val fields = corpus.toAnkiFields()
        Timber.d("Previewing note with fields: ${fields.toList()}")

        helper.api.previewNewNote(
            modelId,
            fields
        ) ?: error("Preview failed")
    }

    override suspend fun sendListToAnki(
        title: String, wordLang: String, meaningLang: String,
        corpora: List<Corpus>,
        onProgress: (Int, Int) -> Unit
    ): Result<Pair<Int, Int>> = runCatching {
        var success = 0
        var failed = 0

        corpora.forEachIndexed { index, corpus ->
            syncCorpusToAnki(title, wordLang, meaningLang, corpus)
                .onSuccess {
                    success++
                }.onFailure {
                    failed++
                    Timber.e(it)
                }
            onProgress(index + 1, corpora.size)
        }

        Timber.d("Success: $success, Failed: $failed")
        success to failed
    }

    override suspend fun markUnexported(corpusIds: List<String>) {
        corpusDao.updateAnkiNoteId(corpusIds, null)
        corpusDao.markExported(corpusIds, null)
    }

    override suspend fun getExportedByNote(noteId: String): List<Corpus> {
        return corpusDao.getExportedByNote(noteId).map { it.asExternalModel() }
    }

    override fun getNotExportedByNote(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getNotExportedByNote(noteId).map { list ->
            list.map { it.asExternalModel() }
        }
    }

    override fun getNoteExportStats(): Flow<List<NoteExportStat>> {
        return noteDao.getNoteExportStats()
    }

    // ---------------- PRIVATE ----------------

    private fun getOrCreateModelId(deckId: Long): Long {
        return helper.findModelIdByName(
            AnkiDroidConfig.MODEL_NAME,
            AnkiDroidConfig.FIELDS.size
        ) ?: helper.api.addNewCustomModel(
            AnkiDroidConfig.MODEL_NAME,
            AnkiDroidConfig.FIELDS,
            AnkiDroidConfig.CARD_NAMES,
            AnkiDroidConfig.QFMT,
            AnkiDroidConfig.AFMT,
            AnkiDroidConfig.CSS,
            deckId,
            null
        ).also {
            helper.storeModelReference(AnkiDroidConfig.MODEL_NAME, it)
        }
    }

    private suspend fun syncCorpusToAnki(
        title: String, wordLang: String, meaningLang: String,
        corpus: Corpus,
    ): Result<Long> = withContext(Dispatchers.IO) {
        val deckId: Long = helper.getOrCreateDeckIdForNote(title, wordLang, meaningLang)
        val modelId: Long = getOrCreateModelId(deckId)
        val fields: Array<String> = corpus.toAnkiFields()
        val tags: Set<String> = generateTags(corpus)

        Timber.d("Syncing ${corpus.word}: ${corpus.ankiNoteId}")

        corpus.ankiNoteId?.let { noteId ->
            val updated = helper.updateNote(
                noteId = noteId,
                fields = fields,
                tags = tags
            )

            return@withContext if (updated) {
                markExported(corpus)
                Result.success(noteId)
            } else {
                Result.failure(Exception("Failed to update Anki note for ${corpus.word}"))
            }
        }

        val newNoteId = helper.addNote(
            modelId = modelId,
            deckId = deckId,
            fields = fields,
            tags = tags
        ) ?: return@withContext Result.failure(
            Exception("Failed to add Anki note")
        )

        corpusDao.updateAnkiNoteId(
            corpusIds = listOf(corpus.id),
            ankiNoteId = newNoteId
        )
        markExported(corpus)

        Result.success(newNoteId)
    }

    private suspend fun markExported(corpus: Corpus) = corpusDao.markExported(
        corpusIds = listOf(corpus.id),
        timestamp = System.currentTimeMillis()
    )

}

interface AnkiRepository {

    fun isAnkiAvailable(): Boolean

    fun checkDeckExists(title: String, wordLang: String, meaningLang: String): Boolean

    suspend fun previewFirstNote(
        title: String, wordLang: String, meaningLang: String,
        corpus: Corpus
    ): Result<Map<String, Map<String, String>>>

    suspend fun sendListToAnki(
        title: String, wordLang: String, meaningLang: String,
        corpora: List<Corpus>,
        onProgress: (Int, Int) -> Unit
    ): Result<Pair<Int, Int>>

    suspend fun markUnexported(corpusIds: List<String>)
    suspend fun getExportedByNote(noteId: String): List<Corpus>
    fun getNotExportedByNote(noteId: String): Flow<List<Corpus>>
    fun getNoteExportStats(): Flow<List<NoteExportStat>>
}