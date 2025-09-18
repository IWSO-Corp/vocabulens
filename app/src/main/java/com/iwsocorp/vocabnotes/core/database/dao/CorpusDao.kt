package com.iwsocorp.vocabnotes.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CorpusDao {

    @Query("""
        SELECT COUNT(*) 
        FROM corpus 
        WHERE word IN (:words)
    """)
    suspend fun countExisting(words: List<String>): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCorpus(corpus: CorpusEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCorpusList(corpusList: List<CorpusEntity>): List<Long>

    @Update
    suspend fun updateCorpus(corpus: CorpusEntity)

    @Query("DELETE FROM corpus WHERE word = :word")
    suspend fun deleteCorpusById(word: String)

    @Query("SELECT * FROM corpus WHERE word = :word")
    suspend fun getCorpusByWord(word: String): CorpusEntity

    @Query("SELECT * FROM corpus WHERE word LIKE :query || '%'")
    fun searchCorpus(query: String): PagingSource<Int, CorpusEntity>

    @Query("SELECT * FROM corpus ORDER BY word ASC")
    fun getAllCorpus(): PagingSource<Int, CorpusEntity>

    @Query("SELECT COUNT(*) FROM corpus")
    fun allCorpusSize(): Flow<Int>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId ORDER BY updatedAt DESC LIMIT 5")
    fun getLatestCorpus(noteId: String): Flow<List<CorpusEntity>>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId ORDER BY word ASC")
    fun getCorpusByNoteId(noteId: String): PagingSource<Int, CorpusEntity>

    @Query("DELETE FROM corpus WHERE noteId = :noteId")
    suspend fun deleteCorpusByNoteId(noteId: String)

}

// Extension / Helper function
suspend fun CorpusDao.insertCorpusListWithResult(
    corpusList: List<CorpusEntity>
): InsertResult {
    val resultIds = insertCorpusList(corpusList)
    val successCount = resultIds.count { it != -1L }
    val failedCount = resultIds.count { it == -1L }
    return InsertResult(successCount, failedCount)
}

// Data class untuk menampung hasil
data class InsertResult(
    val successCount: Int,
    val failedCount: Int
)