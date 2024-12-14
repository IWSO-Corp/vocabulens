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

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insertCorpus(corpus: CorpusEntity)

    @Insert(onConflict = OnConflictStrategy.NONE)
    suspend fun insertCorpusList(corpusList: List<CorpusEntity>)

    @Update
    suspend fun updateCorpus(corpus: CorpusEntity)

    @Query("DELETE FROM corpus WHERE id = :id")
    suspend fun deleteCorpusById(id: String)

    @Query("SELECT * FROM corpus WHERE word = :word")
    suspend fun getCorpusByWord(word: String): CorpusEntity

    @Query("SELECT * FROM corpus WHERE word LIKE :query || '%'")
    fun searchCorpus(query: String): PagingSource<Int, CorpusEntity>

    @Query("SELECT * FROM corpus ORDER BY word ASC")
    fun getAllCorpus(): PagingSource<Int, CorpusEntity>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId ORDER BY updatedAt DESC LIMIT 5")
    fun getLatestCorpus(noteId: String): Flow<List<CorpusEntity>>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId ORDER BY word ASC")
    fun getCorpusByNoteId(noteId: String): PagingSource<Int, CorpusEntity>

    @Query("DELETE FROM corpus WHERE noteId = :noteId")
    suspend fun deleteCorpusByNoteId(noteId: String)

}