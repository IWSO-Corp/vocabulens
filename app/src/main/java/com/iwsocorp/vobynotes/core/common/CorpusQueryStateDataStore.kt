package com.iwsocorp.vobynotes.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val APP_LANG = stringPreferencesKey("app_lang")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TRANSLATION_LANG = stringPreferencesKey("translation_lang")
        val THEME = stringPreferencesKey("theme")
        val DATA_USAGE = booleanPreferencesKey("data_usage")
    }

    val appLang: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LANG]
    }

    suspend fun setAppLang(lang: String) = context.dataStore.edit { prefs ->
        prefs[Keys.APP_LANG] = lang
    }

    val sourceLang: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SOURCE_LANG]
    }

    suspend fun setSourceLang(lang: String) = context.dataStore.edit { prefs ->
        prefs[Keys.SOURCE_LANG] = lang
    }

    val translationLang: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.TRANSLATION_LANG]
    }

    suspend fun setTranslationLang(lang: String) = context.dataStore.edit { prefs ->
        prefs[Keys.TRANSLATION_LANG] = lang
    }

    val theme: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME] ?: ""
    }

    suspend fun switchLanguage() = context.dataStore.edit { prefs ->
        val sourceLang = prefs[Keys.SOURCE_LANG]
        val translationLang = prefs[Keys.TRANSLATION_LANG]
        prefs[Keys.SOURCE_LANG] = translationLang ?: ""
        prefs[Keys.TRANSLATION_LANG] = sourceLang ?: ""
    }

    suspend fun setTheme(theme: String) = context.dataStore.edit { prefs ->
        prefs[Keys.THEME] = theme
    }

    val isInfoShowed: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[Keys.DATA_USAGE]
    }

    suspend fun setInfoShowed(isInfoShowed: Boolean?) = context.dataStore.edit { prefs ->
        if (isInfoShowed == null) prefs.remove(Keys.DATA_USAGE)
        else prefs[Keys.DATA_USAGE] = isInfoShowed
    }

    val queryState: Flow<CorpusQueryState> = context.dataStore.data.map { prefs ->
        val mark = prefs[Keys.MARK]?.let { Mark.valueOf(it) }
        val sortBy = prefs[Keys.SORT_BY]?.let { SortBy.valueOf(it) } ?: SortBy.WORD
        val sortOrder = prefs[Keys.SORT_ORDER]?.let { SortOrder.valueOf(it) } ?: SortOrder.ASC
        Timber.d("queryState: $mark, $sortBy, $sortOrder")
        CorpusQueryState(mark, sortBy, sortOrder)
    }

    suspend fun saveQueryState(state: CorpusQueryState) = context.dataStore.edit { prefs ->
        state.mark?.let { prefs[Keys.MARK] = it.name } ?: prefs.remove(Keys.MARK)
        prefs[Keys.SORT_BY] = state.sortBy.name
        prefs[Keys.SORT_ORDER] = state.sortOrder.name
    }
}

