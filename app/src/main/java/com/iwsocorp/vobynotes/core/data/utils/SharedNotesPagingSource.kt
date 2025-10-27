package com.iwsocorp.vobynotes.core.data.utils

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
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
        return try {
            // ✅ Filter dinamis
            if (!filterWordLang.isNullOrEmpty()) {
                query = query.whereEqualTo("wordLang", filterWordLang)
            }
            if (!filterMeaningLang.isNullOrEmpty()) {
                query = query.whereEqualTo("meaningLang", filterMeaningLang)
            }

            // ✅ Sorting
            query = query.orderBy(sortBy, sortDirection).limit(params.loadSize.toLong())

            // ✅ Paging
            if (params.key != null) {
                query = query.startAfter(params.key!!)
            }

            val snapshot = query.get().await()
            val notes = snapshot.toObjects(SharedNote::class.java)
            val lastVisible = snapshot.documents.lastOrNull()

            Timber.d("load() called with: notes = $notes")

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