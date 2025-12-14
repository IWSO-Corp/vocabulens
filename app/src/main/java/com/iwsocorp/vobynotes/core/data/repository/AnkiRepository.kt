package com.iwsocorp.vobynotes.core.data.repository

import android.app.Activity
import android.content.Context
import com.iwsocorp.vobynotes.core.data.anki.AnkiDroidConfig
import com.iwsocorp.vobynotes.core.data.anki.AnkiDroidHelper
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper.toAnkiFields
import com.iwsocorp.vobynotes.core.database.dao.CorpusDao
import com.iwsocorp.vobynotes.core.database.model.asExternalModel
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.LinkedList
import javax.inject.Inject

class AnkiRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val corpusDao: CorpusDao,
) : AnkiRepository {

    private val helper = AnkiDroidHelper(context)

    override fun isAnkiAvailable(): Boolean {
        return AnkiDroidHelper.isApiAvailable(helper.context)
    }

    override fun shouldRequestPermission(): Boolean {
        return helper.shouldRequestPermission()
    }

    override fun requestPermission(activity: Activity) {
        helper.requestPermission(activity)
    }

    override fun checkDeckExists(note: Note): Boolean {
        return helper.checkDeckExists(note)
    }

    override fun addCorpusToNoteDeck(note: Note, corpus: Corpus): Result<ExportResult> =
        runCatching {

            // 1️⃣ Deck berdasarkan Note
            val deckId = helper.getOrCreateDeckIdForNote(note)

            // 2️⃣ Model global Vocabulens
            val modelId = getOrCreateModelId(deckId)

            val fieldNames = helper.api.getFieldList(modelId)
                ?: error("Failed to fetch model fields")
            Timber.d("Field Name: ${fieldNames.toList()}")

            // 3️⃣ Mapping Corpus → Anki Cards
            val cards = CorpusToAnkiMapper.mapToCards(corpus)

            val fields = LinkedList<Array<String?>>()
            val tags = LinkedList<Set<String>>()

            cards.forEach { (fieldMap, tagSet) ->
                Timber.d("Field Map: $fieldMap")
                Timber.d("Tag Set: $tagSet")

                val row = arrayOfNulls<String>(fieldNames.size)

                fieldNames.indices.forEach { idx ->
                    if (idx < AnkiDroidConfig.FIELDS.size) {
                        row[idx] = fieldMap[AnkiDroidConfig.FIELDS[idx]]
                    }
                }

                fields += row
                row.last().takeIf { it != "-" }?.let {
                    tags += tagSet + setOf(it)
                } ?: run {
                    tags += tagSet
                }
            }

            fields.forEach {
                Timber.d("Fields: ${it.toList()}")
            }
            Timber.d("Tags: $tags")

            // 4️⃣ Remove duplicate (per model, per note)
            helper.removeDuplicates(fields, tags, modelId)

            Timber.d("Fields after removing duplicates: $fields")
            Timber.d("Tags after removing duplicates: $tags")

            if (fields.isEmpty()) {
                Timber.d("All notes are duplicates, skip export")
                ExportResult.SkippedDuplicate
            }

            val added = helper.api.addNotes(
                modelId,
                deckId,
                fields,
                tags
            )

            if (added <= 0) {
                Timber.d("Failed to add notes")
                ExportResult.Failed
            }

            Timber.d("Successfully added $added notes")
            ExportResult.Success(added)
        }

    override suspend fun previewFirstNote(
        note: Note,
        corpus: Corpus
    ): Result<Map<String, Map<String, String>>> = runCatching {
        val deckId = helper.getOrCreateDeckIdForNote(note)
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

    override suspend fun markExported(corpusIds: List<String>, timestamp: Long?) {
        corpusDao.markExported(corpusIds, timestamp)
    }

    override suspend fun getNotExportedByNote(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getNotExportedByNote(noteId).map { list ->
            list.map { it.asExternalModel() }
        }
    }

    override suspend fun getExportedByNote(noteId: String): List<Corpus> {
        return corpusDao.getExportedByNote(noteId).map { it.asExternalModel() }
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
}

interface AnkiRepository {

    fun isAnkiAvailable(): Boolean

    fun shouldRequestPermission(): Boolean

    fun requestPermission(activity: Activity)

    fun checkDeckExists(note: Note): Boolean

    /**
     * Menambahkan daftar kartu ke Anki
     * @return jumlah kartu yang berhasil ditambahkan
     */
    fun addCorpusToNoteDeck(note: Note, corpus: Corpus): Result<ExportResult>

    suspend fun previewFirstNote(
        note: Note,
        corpus: Corpus
    ): Result<Map<String, Map<String, String>>>

    suspend fun markExported(corpusIds: List<String>, timestamp: Long?)
    suspend fun getNotExportedByNote(noteId: String): Flow<List<Corpus>>
    suspend fun getExportedByNote(noteId: String): List<Corpus>

}

sealed class ExportResult {
    data class Success(val added: Int) : ExportResult()
    object SkippedDuplicate : ExportResult()
    object Failed : ExportResult()
}