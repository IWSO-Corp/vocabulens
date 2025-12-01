package com.iwsocorp.vobynotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
    private val firestore: FirebaseFirestore,
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
        val document = docRef.get().await()
        if (!document.exists()) return null

        return createSharedNote(document)
    }

    private fun createSharedNote(document: DocumentSnapshot): SharedNote {
        val corpusJson = document.getString("content")
        val corpusType = object : TypeToken<List<SharedCorpus>>() {}.type
        val corpusList: List<SharedCorpus> = Gson().fromJson(corpusJson, corpusType)
        return SharedNote(
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

    suspend fun checkNoteSavedStatus(userId: String, noteId: String): Boolean {
        val userSaveRef = firestore.collection("user_saves")
            .document(userId)
            .collection("notes")
            .document(noteId)
            .get()
            .await()
        return userSaveRef.exists()
    }

    fun getUserSharedNotes(userId: String, callback: (List<SharedNote>) -> Unit) {
        val userNotesRef = collection
            .whereEqualTo("ownerId", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
        userNotesRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Timber.e("Error getting user shared notes: $exception")
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val sharedNotes = snapshot.documents.mapNotNull { doc ->
                    createSharedNote(doc)
                }
                callback(sharedNotes)
                Timber.d("User shared notes: $sharedNotes")
            } else {
                Timber.d("User has no shared notes")
            }
        }
    }

    suspend fun deleteSharedNote(noteId: String) {
        val userSaves = firestore.collection("user_saves").document().collection("notes")
            .document(noteId).get().await()
        if (userSaves.exists()) {
            userSaves.reference.delete().await()
        }

        val docRef = collection.document(noteId)

        suspendCancellableCoroutine { cont ->
            docRef.delete().addOnSuccessListener {
                cont.resumeWith(Result.success(Unit))
                Timber.d("Delete shared note berhasil")
            }.addOnFailureListener {
                cont.resumeWith(Result.failure(it))
                Timber.e("Delete shared note gagal: $it")
            }
        }
    }

    suspend fun deleteAccountSharedNotes(userId: String) {
        val query = collection
            .whereEqualTo("ownerId", userId)
            .get()
            .await()
        val batch = firestore.batch()
        query.documents.forEach { doc ->
            batch.delete(doc.reference)
        }
        batch.commit().await()
        deleteUserSaves(userId)
    }

    private suspend fun deleteUserSaves(userId: String) {
        val query = firestore.collection("user_saves")
            .document(userId)
            .collection("notes")
        query.get().await().documents.forEach { doc ->
            doc.reference.delete().await()
        }
    }

}