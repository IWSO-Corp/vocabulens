package com.iwsocorp.vobynotes.core.data.repository

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iwsocorp.vobynotes.core.database.dao.CorpusDao
import com.iwsocorp.vobynotes.core.database.dao.ExampleDao
import com.iwsocorp.vobynotes.core.database.dao.NoteDao
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import com.iwsocorp.vobynotes.core.database.model.NoteEntity
import com.iwsocorp.vobynotes.ui.setting.BackupData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val corpusDao: CorpusDao,
    private val noteDao: NoteDao,
    private val exampleDao: ExampleDao,
    private val firestore: FirebaseFirestore,
) {

    suspend fun backupToFirestore(userId: String) {
        backupNotes(userId)
        syncCorpusGrouped(userId)
        backupExamples(userId)
    }

    suspend fun insertToDatabase(backupData: BackupData) = with(backupData) {
        if (notes.isNotEmpty()) noteDao.insertNoteList(notes)
        if (corpus.isNotEmpty()) corpusDao.insertCorpusList(corpus)
        if (examples.isNotEmpty()) exampleDao.insertExampleList(examples)
    }

    private fun userBackupPath(userId: String): DocumentReference = firestore
        .collection("backups")
        .document(userId)

    private suspend fun backupNotes(userId: String) {
        val allNotes = noteDao.getNoteList()
        if (allNotes.isEmpty()) return
        val json = Gson().toJson(allNotes)

        val docRef = userBackupPath(userId)
            .collection("notes")
            .document("notes_data")

        suspendCancellableCoroutine { cont ->
            docRef.set(
                mapOf(
                    "data" to json,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener {
                cont.resumeWith(Result.success(Unit))
                Timber.d("Backup notes berhasil")
            }.addOnFailureListener {
                cont.resumeWith(Result.failure(it))
                Timber.e("Backup notes gagal: $it")
            }
        }
    }

    private suspend fun backupExamples(userId: String) {
        val allExamples = exampleDao.getAll()
        if (allExamples.isEmpty()) return
        val json = Gson().toJson(allExamples)

        val docRef = userBackupPath(userId)
            .collection("examples")
            .document("examples_data")

        suspendCancellableCoroutine { cont ->
            docRef.set(
                mapOf(
                    "data" to json,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).addOnSuccessListener {
                cont.resumeWith(Result.success(Unit))
                Timber.d("Backup examples berhasil")
            }.addOnFailureListener {
                cont.resumeWith(Result.failure(it))
                Timber.e("Backup examples gagal: $it")
            }
        }
    }

    private suspend fun syncCorpusGrouped(userId: String) {
        val allData = corpusDao.getAll()
        if (allData.isEmpty()) return
        val grouped = allData.groupBy { it.noteId }

        grouped.forEach { (noteId, list) ->
            val json = Gson().toJson(list)
            val docRef = userBackupPath(userId)
                .collection("corpus")
                .document(noteId)

            suspendCancellableCoroutine { cont ->
                docRef.set(
                    mapOf(
                        "data" to json,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).addOnSuccessListener {
                    cont.resumeWith(Result.success(Unit))
                    Timber.d("Backup noteId=$noteId berhasil")
                }.addOnFailureListener {
                    cont.resumeWith(Result.failure(it))
                    Timber.e("Backup noteId=$noteId gagal: $it")
                }
            }
        }
    }

    suspend fun restoreAllNotes(userId: String): List<NoteEntity> {
        val docRef = userBackupPath(userId)
            .collection("notes")
            .document("notes_data")
            .get()
            .await()
        val json = docRef.getString("data") ?: return emptyList()
        val type = object : TypeToken<List<NoteEntity>>() {}.type

        return Gson().fromJson(json, type)
    }

    suspend fun restoreAllExamples(userId: String): List<ExampleEntity> {
        val snapshot = userBackupPath(userId)
            .collection("examples")
            .get()
            .await()
        val json = snapshot.documents.firstOrNull()?.getString("data") ?: return emptyList()
        val type = object : TypeToken<List<ExampleEntity>>() {}.type

        return Gson().fromJson(json, type)
    }

    suspend fun restoreAllCorpus(userId: String): List<CorpusEntity> {
        val snapshot = userBackupPath(userId)
            .collection("corpus")
            .get()
            .await()
        val allEntities = mutableListOf<CorpusEntity>()
        val type = object : TypeToken<List<CorpusEntity>>() {}.type

        snapshot.documents.forEach { doc ->
            val json = doc.getString("data") ?: return@forEach
            val entities: List<CorpusEntity> = Gson().fromJson(json, type)
            allEntities.addAll(entities)
        }

        return allEntities
    }

    suspend fun getLocalData(): BackupData {
        val notes = noteDao.getNoteList()
        val corpus = corpusDao.getAll()
        val examples = exampleDao.getAll()
        return BackupData(notes, corpus, examples)
    }

}
