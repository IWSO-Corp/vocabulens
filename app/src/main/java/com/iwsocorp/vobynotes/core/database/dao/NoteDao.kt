package com.iwsocorp.vobynotes.core.database.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import com.iwsocorp.vobynotes.core.database.model.NoteEntity
import com.iwsocorp.vobynotes.core.model.Mark
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteList(noteList: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET title = :title, wordLang = :wordLang, meaningLang = :meaningLang, contentSize = :contentSize, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun refreshSavedNote(
        noteId: String,
        title: String,
        wordLang: String,
        meaningLang: String,
        contentSize: Int,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE notes SET shared = :shared WHERE id = :noteId")
    suspend fun updateNoteSharedStatus(noteId: String, shared: Boolean)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNoteByIds(ids: List<String>)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity

    @Query(
        """
        SELECT n.*, COUNT(c.id) AS corpus_count
        FROM notes AS n
        LEFT JOIN corpus AS c ON n.id = c.noteId
        WHERE n.deletedAt IS NULL
        GROUP BY n.id
        ORDER BY n.updatedAt DESC
    """
    )
    fun getNotesWithCorpusCountFlow(): Flow<List<NoteWithCorpusCount>>

    @Query("SELECT * FROM corpus WHERE deletedAt IS NULL")
    fun getAllCorpusFlow(): Flow<List<CorpusEntity>>

    @Query("SELECT * FROM corpus WHERE deletedAt IS NOT NULL")
    fun getAllTrashCorpusFlow(): Flow<List<CorpusEntity>>

    @Query(
        """
        SELECT n.*, COUNT(c.id) AS corpus_count
        FROM notes AS n
        LEFT JOIN corpus AS c ON n.id = c.noteId
        WHERE n.deletedAt IS NOT NULL
        GROUP BY n.id
        ORDER BY n.updatedAt DESC
    """
    )
    fun getTrashNotesFlow(): Flow<List<NoteWithCorpusCount>>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId ORDER BY updatedAt DESC LIMIT 5")
    suspend fun getLastFiveCorpusByNoteIdSuspend(noteId: String): List<CorpusEntity>

    @Query("SELECT COUNT(*) FROM corpus WHERE noteId = :noteId AND mark = :mark")
    suspend fun countNoteContentMark(noteId: String, mark: Mark): Int

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getNoteList(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getNotesFlow(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET contentSize = contentSize + :count, updatedAt = :time WHERE id = :noteId")
    suspend fun incrementContentSize(
        noteId: String,
        count: Int,
        time: Long = System.currentTimeMillis()
    )

    @Query("UPDATE notes SET contentSize = contentSize - :count WHERE id = :noteId")
    suspend fun decrementContentSize(noteId: String, count: Int)

    @Query("UPDATE notes SET updatedAt = :time WHERE id = :noteId")
    suspend fun updateTimestamp(noteId: String, time: Long)

    @Query("UPDATE notes SET deletedAt = :deletedAt WHERE id IN (:noteIds)")
    suspend fun markNoteDeleted(noteIds: List<String>, deletedAt: Long)

    @Query(
        """
            UPDATE corpus SET deletedAt = :deletedAt
            WHERE noteId IN (:noteIds)
    """
    )
    suspend fun markCorpusDeletedByNoteId(noteIds: List<String>, deletedAt: Long)

    @Query(
        """
            UPDATE examples SET deletedAt = :deletedAt
            WHERE corpusId IN (SELECT id FROM corpus WHERE noteId = :noteIds)
    """
    )
    suspend fun markExampleDeletedByNoteId(noteIds: List<String>, deletedAt: Long)

    @Transaction
    suspend fun moveNotesToTrash(noteIds: List<String>) {
        val deletedAt = System.currentTimeMillis()
        markNoteDeleted(noteIds, deletedAt)
        markCorpusDeletedByNoteId(noteIds, deletedAt)
        markExampleDeletedByNoteId(noteIds, deletedAt)
    }

    @Query("UPDATE notes SET deletedAt = NULL WHERE id IN (:noteIds)")
    suspend fun markNoteRestored(noteIds: List<String>)

    @Query("UPDATE corpus SET deletedAt = NULL WHERE noteId IN (:noteIds)")
    suspend fun markCorpusRestoredByNoteId(noteIds: List<String>)

    @Query(
        """
            UPDATE examples SET deletedAt = NULL
            WHERE corpusId IN (SELECT id FROM corpus WHERE noteId = :noteIds)
    """
    )
    suspend fun markExampleRestoredByNoteId(noteIds: List<String>)

    @Transaction
    suspend fun restoreNotesFromTrash(noteIds: List<String>) {
        markNoteRestored(noteIds)
        markCorpusRestoredByNoteId(noteIds)
        markExampleRestoredByNoteId(noteIds)
    }

    @Query("DELETE FROM examples WHERE deletedAt IS NOT NULL AND deletedAt <= :expiredTime")
    suspend fun deleteExpiredExamples(expiredTime: Long)

    @Query("DELETE FROM corpus WHERE deletedAt IS NOT NULL AND deletedAt <= :expiredTime")
    suspend fun deleteExpiredCorpus(expiredTime: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt <= :expiredTime")
    suspend fun deleteExpiredNotes(expiredTime: Long)

    @Transaction
    suspend fun deleteExpiredData() {
        val expiredTime = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000 // 30 days
        deleteExpiredExamples(expiredTime)
        deleteExpiredCorpus(expiredTime)
        deleteExpiredNotes(expiredTime)
    }
}

data class NoteWithCorpusCount(
    @Embedded val note: NoteEntity,
    @ColumnInfo(name = "corpus_count") val corpusCount: Int,
)

data class NoteWithLatestCorpus(
    @Embedded val note: NoteEntity,
    @ColumnInfo(name = "corpus_count") val corpusCount: Int,
    @ColumnInfo(name = "familiar_count") val familiarCount: Int,
    @ColumnInfo(name = "unfamiliar_count") val unfamiliarCount: Int,
    @Relation(
        parentColumn = "id",
        entityColumn = "noteId",
        entity = CorpusEntity::class
    ) val corpus: List<CorpusEntity>
)