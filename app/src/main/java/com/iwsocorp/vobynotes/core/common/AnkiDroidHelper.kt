package com.iwsocorp.vobynotes.core.common

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.util.SparseArray
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ichi2.anki.api.AddContentApi
import com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION
import com.ichi2.anki.api.NoteInfo
import com.iwsocorp.vobynotes.core.data.anki.AnkiDeckNameResolver
import com.iwsocorp.vobynotes.core.model.Note
import java.util.LinkedList

class AnkiDroidHelper(
    val context: Context
) {
    val api = AddContentApi(context.applicationContext)

    companion object {
        private const val DECK_REF_DB = "com.ichi2.anki.api.decks"
        private const val MODEL_REF_DB = "com.ichi2.anki.api.models"

        fun isApiAvailable(context: Context): Boolean {
            return AddContentApi.getAnkiDroidPackageName(context) != null
        }
    }

    private val appContext: Context = context.applicationContext

    // --------------------------------------------------
    // Permission handling
    // --------------------------------------------------

    fun shouldRequestPermission(): Boolean =
        Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(
            context,
            READ_WRITE_PERMISSION
        ) != PackageManager.PERMISSION_GRANTED

    fun requestPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(READ_WRITE_PERMISSION),
            7001
        )
    }

    // --------------------------------------------------
    // SharedPreferences helpers
    // --------------------------------------------------

    fun storeDeckReference(deckName: String, deckId: Long) {
        decksPrefs().edit()
            .putLong(deckName, deckId)
            .apply()
    }

    fun storeModelReference(modelName: String, modelId: Long) {
        modelsPrefs().edit()
            .putLong(modelName, modelId)
            .apply()
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

        if (duplicates.size() == 0) return

        val fieldIterator = fields.listIterator()
        val tagIterator = tags.listIterator()

        var currentIndex = -1
        for (i in 0 until duplicates.size()) {
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

    fun getOrCreateDeckIdForNote(
        note: Note
    ): Long {
        val deckName = AnkiDeckNameResolver.fromNote(note)

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
}