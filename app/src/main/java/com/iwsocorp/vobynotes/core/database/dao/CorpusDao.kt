package com.iwsocorp.vobynotes.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import com.iwsocorp.vobynotes.core.model.Mark
import kotlinx.coroutines.flow.Flow

@Dao
interface CorpusDao {

    @Query(
        """
        SELECT * FROM corpus
        WHERE (:noteId = '' OR noteId = :noteId)
        AND (:mark IS NULL OR mark = :mark)
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN createdAt END ASC,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN createdAt END DESC,
            CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'ASC' THEN updatedAt END ASC,
            CASE WHEN :sortBy = 'updatedAt' AND :sortOrder = 'DESC' THEN updatedAt END DESC,
            CASE WHEN :sortBy = 'word' AND :sortOrder = 'ASC' THEN word END ASC,
            CASE WHEN :sortBy = 'word' AND :sortOrder = 'DESC' THEN word END DESC
    """
    )
    fun getPagedCorpus(
        noteId: String,
        mark: Mark?,
        sortBy: String,
        sortOrder: String,
    ): PagingSource<Int, CorpusEntity>

    @Query("SELECT COUNT(*) FROM corpus WHERE word IN (:words)")
    suspend fun countExisting(words: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insertCorpus(corpus: CorpusEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCorpusList(corpusList: List<CorpusEntity>): List<Long>

    @Update
    suspend fun updateCorpus(corpus: CorpusEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExamples(examples: List<ExampleEntity>)

    @Transaction
    suspend fun updateCorpusAndInsertExamples(
        corpus: CorpusEntity,
        examples: List<ExampleEntity>
    ) {
        updateCorpus(corpus)
        insertExamples(examples)
    }

    @Query("DELETE FROM corpus WHERE id IN (:ids)")
    suspend fun deleteBatch(ids: List<String>)

    @Query("SELECT * FROM corpus WHERE word = :word AND deletedAt IS NULL")
    suspend fun getCorpusByWord(word: String): CorpusEntity?

    @Query("SELECT * FROM corpus WHERE id = :id")
    fun getCorpusById(id: String): Flow<CorpusEntity>

    @Query("SELECT * FROM corpus WHERE deletedAt IS NULL AND word LIKE :query || '%'")
    fun searchCorpus(query: String): PagingSource<Int, CorpusEntity>

    @Query("SELECT * FROM corpus WHERE deletedAt IS NULL ORDER BY word ASC")
    fun getAllCorpus(): PagingSource<Int, CorpusEntity>

    @Query("SELECT * FROM corpus WHERE deletedAt IS NULL")
    fun allCorpusFlow(): Flow<List<CorpusEntity>>

    @Query("SELECT * FROM corpus WHERE deletedAt IS NULL")
    suspend fun getAll(): List<CorpusEntity>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId ORDER BY word ASC")
    fun getCorpusByNoteId(noteId: String): PagingSource<Int, CorpusEntity>

    @Query("DELETE FROM corpus WHERE noteId = :noteId")
    suspend fun deleteCorpusByNoteId(noteId: String)

    @Query("UPDATE corpus SET noteId = :newNoteId WHERE id IN (:corpusIds)")
    suspend fun moveCorpusToNote(corpusIds: List<String>, newNoteId: String)

    @Query("UPDATE corpus SET mark = :newMark WHERE id IN (:corpusIds)")
    suspend fun updateCorpusMark(corpusIds: List<String>, newMark: Mark)

}

suspend fun CorpusDao.insertCorpusListWithResult(
    corpusList: List<CorpusEntity>,
): InsertResult {
    val resultIds = insertCorpusList(corpusList)
    val successCount = resultIds.count { it != -1L }
    val failedCount = resultIds.count { it == -1L }
    return InsertResult(successCount, failedCount)
}

data class InsertResult(
    val successCount: Int,
    val failedCount: Int,
)