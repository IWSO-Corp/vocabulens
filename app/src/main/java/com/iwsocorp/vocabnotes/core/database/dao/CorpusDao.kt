package com.iwsocorp.vocabnotes.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CorpusDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCorpus(corpus: CorpusEntity)

    @Update
    suspend fun updateCorpus(corpus: CorpusEntity)

    @Query("DELETE FROM corpus WHERE id = :id")
    suspend fun deleteCorpusById(id: String)

    @Query("SELECT * FROM corpus WHERE word = :word")
    suspend fun getCorpusByWord(word: String): CorpusEntity

    @Query("SELECT * FROM corpus WHERE word LIKE '%' || :query || '%'")
    fun searchCorpus(query: String): Flow<List<CorpusEntity>>

    @Query("SELECT * FROM corpus")
    fun getAllCorpus(): Flow<List<CorpusEntity>>

    @Query("SELECT * FROM corpus WHERE noteId = :noteId")
    fun getCorpusByNoteId(noteId: String): Flow<List<CorpusEntity>>

    @Query("DELETE FROM corpus WHERE noteId = :noteId")
    suspend fun deleteCorpusByNoteId(noteId: String)

}