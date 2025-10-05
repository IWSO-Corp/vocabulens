package com.iwsocorp.vobynotes.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.SortBy
import com.iwsocorp.vobynotes.core.model.SortOrder
import com.iwsocorp.vobynotes.ui.note.CorpusQueryState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorpusQueryStateDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "corpus_query")

    private object Keys {
        val MARK = stringPreferencesKey("mark")
        val SORT_BY = stringPreferencesKey("sort_by")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    val queryState: Flow<CorpusQueryState> = context.dataStore.data
        .map { prefs ->
            val mark = prefs[Keys.MARK]?.let { Mark.valueOf(it) }
            val sortBy = prefs[Keys.SORT_BY]?.let { SortBy.valueOf(it) } ?: SortBy.WORD
            val sortOrder = prefs[Keys.SORT_ORDER]?.let { SortOrder.valueOf(it) } ?: SortOrder.ASC
            Timber.d("queryState: $mark, $sortBy, $sortOrder")
            CorpusQueryState(mark, sortBy, sortOrder)
        }

    suspend fun saveQueryState(state: CorpusQueryState) {
        context.dataStore.edit { prefs ->
            state.mark?.let { prefs[Keys.MARK] = it.name } ?: prefs.remove(Keys.MARK)
            prefs[Keys.SORT_BY] = state.sortBy.name
            prefs[Keys.SORT_ORDER] = state.sortOrder.name
        }
    }
}

