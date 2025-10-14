package com.iwsocorp.vobynotes.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iwsocorp.vobynotes.core.database.model.ExampleEntity
import com.iwsocorp.vobynotes.core.model.Mark
import kotlinx.coroutines.flow.Flow

@Dao
interface ExampleDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExampleSentence(exampleEntity: ExampleEntity)

    @Query("SELECT * FROM examples WHERE LOWER(sentence) AND deletedAt IS NULL LIKE LOWER('%' || :word || '%') OR forWord = :word")
    fun getExamplesByWord(word: String): Flow<List<ExampleEntity>>

    @Query("SELECT * FROM examples WHERE deletedAt IS NULL")
    suspend fun getAll(): List<ExampleEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExampleList(exampleList: List<ExampleEntity>)

    @Query(
        """
        SELECT e.* FROM examples e
        INNER JOIN Corpus c ON e.forWord = c.word
        WHERE 
            (:noteId IS NULL OR c.noteId = :noteId)
        AND (:mark IS NULL OR c.mark = :mark)
        AND (:wordLang IS NULL OR c.wordLang = :wordLang)
        AND (:meaningLang IS NULL OR c.meaningLang = :meaningLang)
        AND e.deletedAt IS NULL
        ORDER BY RANDOM()
    """
    )
    fun getExamplesForQuiz(
        noteId: String? = null,
        mark: Mark? = null,
        wordLang: String? = null,
        meaningLang: String? = null,
    ): Flow<List<ExampleEntity>>

    @Query("UPDATE examples SET quizCount = quizCount + 1 WHERE id IN (:ids)")
    suspend fun incrementQuizCount(ids: List<String>)

}