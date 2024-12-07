package com.iwsocorp.vocabnotes.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iwsocorp.vocabnotes.core.database.model.MeaningEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeaningDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordMeanings(meaning: List<MeaningEntity>)

    @Query("SELECT * FROM meaning WHERE word = :word")
    fun getWordMeanings(word: String): Flow<List<MeaningEntity>>

}