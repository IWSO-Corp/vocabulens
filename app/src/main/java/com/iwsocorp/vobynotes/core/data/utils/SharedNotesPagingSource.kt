package com.iwsocorp.vobynotes.core.data.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iwsocorp.vobynotes.core.model.SharedCorpus
import com.iwsocorp.vobynotes.core.model.SharedNote
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class SharedNotesPagingSource(
    private var query: Query,
    private val filterWordLang: String? = null,
    private val filterMeaningLang: String? = null,
    private val sortBy: String = "updatedAt",
    private val sortDirection: Query.Direction = Query.Direction.DESCENDING
) : PagingSource<DocumentSnapshot, SharedNote>() {

    override suspend fun load(params: LoadParams<DocumentSnapshot>): LoadResult<DocumentSnapshot, SharedNote> {
        Timber.d("load() called with: params = ${params.loadSize}")
        Timber.d("filterWordLang: ${filterWordLang?.lowercase()}")
        Timber.d("filterMeaningLang: $filterMeaningLang")
        Timber.d("sortBy: $sortBy")
        Timber.d("sortDirection: $sortDirection")

        return try {
            // ✅ Filter dinamis
            if (!filterWordLang.isNullOrEmpty()) {
                query = query.whereEqualTo("wordLang", filterWordLang.lowercase())
            }
            if (!filterMeaningLang.isNullOrEmpty()) {
                query = query.whereEqualTo("meaningLang", filterMeaningLang)
            }

            // ✅ Sorting
            query = if (sortBy.isNotEmpty()) {
                query.orderBy(sortBy, sortDirection)
            } else {
                query.orderBy("updatedAt", Query.Direction.DESCENDING)
            }.limit(params.loadSize.toLong())

            // ✅ Paging
            if (params.key != null) {
                query = query.startAfter(params.key!!)
            }

            Timber.d("Query: ${query.firestore}")

            val snapshot = query.get().await()
            val lastVisible = snapshot.documents.lastOrNull()
            val notes = mutableListOf<SharedNote>()

            snapshot.documents.forEach { document ->
                val corpusJson = document.getString("content")
                val corpusType = object : TypeToken<List<SharedCorpus>>() {}.type
                val corpusList: List<SharedCorpus> = Gson().fromJson(corpusJson, corpusType)
                val note = SharedNote(
                    id = document.getString("id") ?: "",
                    ownerId = document.getString("ownerId") ?: "",
                    ownerAvatar = document.getString("ownerAvatar"),
                    ownerName = document.getString("ownerName"),
                    title = document.getString("title") ?: "",
                    wordLang = document.getString("wordLang") ?: "",
                    meaningLang = document.getString("meaningLang") ?: "",
                    content = corpusList,
                    savedCount = (document.getLong("savedCount") ?: 0).toInt(),
                    uploadedAt = document.getTimestamp("uploadedAt") ?: Timestamp.now(),
                    updatedAt = document.getTimestamp("updatedAt") ?: Timestamp.now(),
                )
                notes.add(note)
            }

            Timber.d("load() called with: notes = ${notes.size}")

            LoadResult.Page(
                data = notes,
                prevKey = null, // hanya forward paging
                nextKey = lastVisible
            )
        } catch (e: Exception) {
            Timber.e(e)
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<DocumentSnapshot, SharedNote>): DocumentSnapshot? {
        return null
    }
}