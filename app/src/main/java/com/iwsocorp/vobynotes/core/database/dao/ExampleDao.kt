package com.iwsocorp.vobynotes.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExampleSentence(exampleEntity: ExampleEntity)

    @Query("SELECT * FROM examples WHERE LOWER(sentence) LIKE LOWER('%' || :word || '%')")
    fun getExamplesByWord(word: String): Flow<List<ExampleEntity>>

}