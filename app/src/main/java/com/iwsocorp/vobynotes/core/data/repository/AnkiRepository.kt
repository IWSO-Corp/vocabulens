package com.iwsocorp.vobynotes.core.data.repository

import android.app.Activity
import android.content.Context
import com.iwsocorp.vobynotes.core.common.AnkiDroidConfig
import com.iwsocorp.vobynotes.core.common.AnkiDroidHelper
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper.toAnkiFields
import com.iwsocorp.vobynotes.core.database.dao.CorpusDao
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Note
import dagger.hilt.android.qualifiers.ApplicationContext
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

    override fun addCorpusToNoteDeck(
        corpus: Corpus,
        note: Note
    ): Result<Int> = runCatching {

        // 1️⃣ Deck berdasarkan Note
        val deckId = helper.getOrCreateDeckIdForNote(note)

        // 2️⃣ Model global Vocabulens
        val modelId = getOrCreateModelId(deckId)

        val fieldNames = helper.api.getFieldList(modelId)
            ?: error("Failed to fetch model fields")

        // 3️⃣ Mapping Corpus → Anki Cards
        val cards = CorpusToAnkiMapper.mapToCards(corpus)

        val fields = LinkedList<Array<String?>>()
        val tags = LinkedList<Set<String>>()

        cards.forEach { (fieldMap, tagSet) ->
            val row = arrayOfNulls<String>(fieldNames.size)

            fieldNames.indices.forEach { idx ->
                if (idx < AnkiDroidConfig.FIELDS.size) {
                    row[idx] = fieldMap[AnkiDroidConfig.FIELDS[idx]]
                }
            }

            fields += row
            tags += tagSet + setOf(
                "note_${note.id}",
                note.wordLang.lowercase(),
                note.meaningLang.lowercase()
            )
        }

        // 4️⃣ Remove duplicate (per model, per note)
        helper.removeDuplicates(fields, tags, modelId)

        val added = helper.api.addNotes(
            modelId,
            deckId,
            fields,
            tags
        )

        if (added <= 0) error("Failed to add cards to Anki")

        added
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
        Timber.d("Previewing note with fields: $fields")

        helper.api.previewNewNote(
            modelId,
            fields
        ) ?: error("Preview failed")
    }

    override suspend fun markExported(corpusIds: List<String>) {
        corpusDao.markExported(corpusIds)
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

    fun requestPermission(activity: android.app.Activity)

    /**
     * Menambahkan daftar kartu ke Anki
     * @return jumlah kartu yang berhasil ditambahkan
     */
    fun addCorpusToNoteDeck(
        corpus: Corpus,
        note: Note
    ): Result<Int>

    suspend fun previewFirstNote(
        note: Note,
        corpus: Corpus
    ): Result<Map<String, Map<String, String>>>

    suspend fun markExported(corpusIds: List<String>)

}