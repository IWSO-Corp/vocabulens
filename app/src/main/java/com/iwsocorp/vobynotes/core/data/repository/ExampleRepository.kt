package com.iwsocorp.vobynotes.core.data.repository

import com.iwsocorp.vobynotes.core.database.dao.ExampleDao
import com.iwsocorp.vobynotes.core.database.model.asExternalModel
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExampleRepositoryImpl @Inject constructor(
    private val exampleDao: ExampleDao,
) : ExampleRepository {

    override suspend fun insertExampleSentence(example: Example) {
        exampleDao.insertExampleSentence(example.asEntity())
    }

    override suspend fun getAll(): List<Example> {
        return exampleDao.getAll().sortedBy { it.quizCount }.map { it.asExternalModel() }
    }

    override fun getExamplesByWord(word: String): Flow<List<Example>> =
        exampleDao.getExamplesByWord(word).map {
            it.map { entity -> entity.asExternalModel() }
        }

    override fun getExamplesForQuiz(
        noteId: String?,
        mark: Mark?,
        wordLang: String?,
        meaningLang: String?,
    ): Flow<List<Example>> = exampleDao.getExamplesForQuiz(
        noteId,
        mark,
        wordLang,
        meaningLang,
    ).map { list ->
        list.map { it.asExternalModel() }
    }

    override suspend fun incrementQuizCount(ids: List<String>) {
        exampleDao.incrementQuizCount(ids)
    }

}

interface ExampleRepository {
    suspend fun insertExampleSentence(example: Example)
    suspend fun getAll(): List<Example>
    fun getExamplesByWord(word: String): Flow<List<Example>>
    fun getExamplesForQuiz(
        noteId: String? = null,
        mark: Mark? = null,
        wordLang: String? = null,
        meaningLang: String? = null,
    ): Flow<List<Example>>

    suspend fun incrementQuizCount(ids: List<String>)
}