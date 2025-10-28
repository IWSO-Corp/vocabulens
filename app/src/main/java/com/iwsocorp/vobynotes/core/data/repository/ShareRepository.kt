package com.iwsocorp.vobynotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.iwsocorp.vobynotes.core.data.utils.SharedNotesPagingSource
import com.iwsocorp.vobynotes.core.model.SharedCorpus
import com.iwsocorp.vobynotes.core.model.SharedNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepository @Inject constructor(
    firestore: FirebaseFirestore,
) {
    private val collection = firestore.collection("shared_notes")
    private val query: Query = collection

    suspend fun shareNoteToPublic(sharedNote: SharedNote) {
        val corpusJson = Gson().toJson(sharedNote.content)
        val docRef = collection.document(sharedNote.id)
        val data = hashMapOf(
            "id" to sharedNote.id,
            "ownerId" to sharedNote.ownerId,
            "ownerAvatar" to sharedNote.ownerAvatar,
            "ownerName" to sharedNote.ownerName,
            "title" to sharedNote.title,
            "wordLang" to sharedNote.wordLang,
            "meaningLang" to sharedNote.meaningLang,
            "content" to corpusJson,
            "savedCount" to sharedNote.savedCount,
            "uploadedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        suspendCancellableCoroutine { cont ->
            docRef.set(data).addOnSuccessListener {
                cont.resumeWith(Result.success(Unit))
                Timber.d("Share note berhasil")
            }.addOnFailureListener {
                cont.resumeWith(Result.failure(it))
                Timber.e("Share note gagal: $it")
            }
        }
    }

    suspend fun updateSharedNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String,
        content: List<SharedCorpus>,
    ) {
        val docRef = collection.document(noteId)
        val updateData = hashMapOf(
            "title" to title,
            "wordLang" to wordLang,
            "meaningLang" to meaningLang,
            "content" to Gson().toJson(content),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        suspendCancellableCoroutine { cont ->
            docRef.update(updateData).addOnSuccessListener {
                cont.resumeWith(Result.success(Unit))
                Timber.d("Update shared note berhasil")
            }.addOnFailureListener {
                cont.resumeWith(Result.failure(it))
                Timber.e("Update shared note gagal: $it")
            }
        }
    }

    fun getPagedSharedNotes(
        filterWordLang: String? = null,
        filterMeaningLang: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: Query.Direction = Query.Direction.DESCENDING
    ): Flow<PagingData<SharedNote>> = Pager(
        config = PagingConfig(
            pageSize = 10,
            enablePlaceholders = false,
            initialLoadSize = 10,
        ),
        pagingSourceFactory = {
            SharedNotesPagingSource(
                query,
                filterWordLang,
                filterMeaningLang,
                sortBy,
                sortDirection,
            )
        }
    ).flow

    suspend fun getSharedNoteById(id: String): SharedNote? {
        val docRef = collection.document(id)
        val snapshot = docRef.get().await()
        return snapshot.toObject(SharedNote::class.java)
    }

    fun saveSharedNoteTransaction(
        userId: String,
        noteId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val noteRef = collection.document(noteId)
        val userSaveRef = firestore.collection("user_saves").document(userId)
            .collection("notes").document(noteId)

        firestore.runTransaction { transaction ->
            val noteSnapshot = transaction.get(noteRef)
            val noteOwner = noteSnapshot.getString("ownerId") ?: ""
            val currentSavedCount = noteSnapshot.getLong("savedCount") ?: 0L

            val userSaveSnapshot = transaction.get(userSaveRef)
            val alreadySaved = userSaveSnapshot.exists()

            if (noteOwner != userId && !alreadySaved) {
                // 🔹 Tambah save baru
                transaction.set(
                    userSaveRef,
                    mapOf(
                        "saved" to true,
                        "savedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(noteRef, "savedCount", currentSavedCount + 1)
            }

            null
        }.addOnSuccessListener {
            callback(Result.success(Unit))
        }.addOnFailureListener { e ->
            callback(Result.failure(e))
        }
    }

}