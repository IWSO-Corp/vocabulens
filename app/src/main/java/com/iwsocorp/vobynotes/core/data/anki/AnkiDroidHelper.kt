package com.iwsocorp.vobynotes.core.data.anki

import android.content.Context
import android.content.SharedPreferences
import android.util.SparseArray
import androidx.core.content.edit
import androidx.core.util.forEach
import androidx.core.util.isEmpty
import androidx.core.util.size
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.NoteInfo
import com.iwsocorp.vobynotes.core.data.mapper.CorpusToAnkiMapper
import timber.log.Timber
import java.util.LinkedList

class AnkiDroidHelper(
    val context: Context
) {
    val api = AddContentApi(context.applicationContext)

    companion object {
        private const val DECK_REF_DB = "com.ichi2.anki.api.decks"
        private const val MODEL_REF_DB = "com.ichi2.anki.api.models"

        fun isApiAvailable(context: Context): Boolean {
            val anki = AddContentApi.getAnkiDroidPackageName(context)
            Timber.d("Anki: $anki")
            return anki != null
        }
    }

    private val appContext: Context = context.applicationContext

    // --------------------------------------------------
    // SharedPreferences helpers
    // --------------------------------------------------

    fun storeDeckReference(deckName: String, deckId: Long) {
        decksPrefs().edit {
            putLong(deckName, deckId)
        }
    }

    fun storeModelReference(modelName: String, modelId: Long) {
        modelsPrefs().edit {
            putLong(modelName, modelId)
        }
    }

    private fun decksPrefs(): SharedPreferences =
        appContext.getSharedPreferences(DECK_REF_DB, Context.MODE_PRIVATE)

    private fun modelsPrefs(): SharedPreferences =
        appContext.getSharedPreferences(MODEL_REF_DB, Context.MODE_PRIVATE)

    // --------------------------------------------------
    // Duplicate handling
    // --------------------------------------------------

    fun removeDuplicates(
        fields: LinkedList<Array<String?>>,
        tags: LinkedList<Set<String>>,
        modelId: Long
    ) {
        require(fields.size == tags.size) {
            "Fields and tags list must have the same size"
        }

        if (fields.isEmpty()) return

        val keys = fields.map { it[0] }
        val duplicates: SparseArray<List<NoteInfo>> =
            api.findDuplicateNotes(modelId, keys) ?: return

        duplicates.forEach { idx, value ->
            Timber.d("Index: $idx")
            value.forEach {
                Timber.d("Duplicate fields: ${it.fields.toList()}")
            }
        }

        if (duplicates.isEmpty()) return

        val fieldIterator = fields.listIterator()
        val tagIterator = tags.listIterator()

        var currentIndex = -1
        for (i in 0 until duplicates.size) {
            val duplicateIndex = duplicates.keyAt(i)
            while (currentIndex < duplicateIndex) {
                fieldIterator.next()
                tagIterator.next()
                currentIndex++
            }
            fieldIterator.remove()
            tagIterator.remove()
        }
    }

    // --------------------------------------------------
    // Model helpers
    // --------------------------------------------------

    fun findModelIdByName(modelName: String, minFields: Int): Long? {
        val storedId = modelsPrefs().getLong(modelName, -1L)

        if (storedId != -1L &&
            api.getModelName(storedId) != null &&
            (api.getFieldList(storedId)?.size ?: 0) >= minFields
        ) {
            return storedId
        }

        val models = api.getModelList(minFields) ?: return null
        return models.entries.firstOrNull { it.value == modelName }?.key
    }

    // --------------------------------------------------
    // Deck helpers
    // --------------------------------------------------

    fun checkDeckExists(title: String, wordLang: String, meaningLang: String): Boolean =
        findDeckIdByName(CorpusToAnkiMapper.makeDeckName(title, wordLang, meaningLang)) != null

    fun getOrCreateDeckIdForNote(title: String, wordLang: String, meaningLang: String): Long {
        val deckName = CorpusToAnkiMapper.makeDeckName(title, wordLang, meaningLang)

        return findDeckIdByName(deckName)
            ?: api.addNewDeck(deckName).also {
                storeDeckReference(deckName, it)
            }
    }

    fun findDeckIdByName(deckName: String): Long? {
        getDeckId(deckName)?.let { return it }

        val storedId = decksPrefs().getLong(deckName, -1L)
        return if (storedId != -1L && api.getDeckName(storedId) != null) {
            storedId
        } else {
            null
        }
    }

    private fun getDeckId(deckName: String): Long? {
        val decks = api.getDeckList() ?: return null
        return decks.entries.firstOrNull {
            it.value.equals(deckName, ignoreCase = true)
        }?.key
    }

    /**
     * Add new note → return ankiNoteId
     */
    fun addNote(
        modelId: Long,
        deckId: Long,
        fields: Array<String>,
        tags: Set<String>
    ): Long? = try {
        api.addNote(
            modelId,
            deckId,
            fields,
            tags
        )
    } catch (e: Exception) {
        Timber.e(e)
        null
    }

    /**
     * Update existing note (ANTI DUPLICATE)
     */
    fun updateNote(
        noteId: Long,
        fields: Array<String>,
        tags: Set<String>
    ): Boolean = try {
        val result1 = api.updateNoteFields(noteId, fields)
        val result2 = api.updateNoteTags(noteId, tags)

        Timber.d("Result 1: $result1")
        Timber.d("Result 2: $result2")

        result1 && result2
    } catch (e: Exception) {
        Timber.e(e)
        false
    }

}