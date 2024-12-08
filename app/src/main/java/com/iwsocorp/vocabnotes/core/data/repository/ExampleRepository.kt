package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.database.dao.ExampleDao
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Example
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExampleRepositoryImpl @Inject constructor(
    private val exampleDao: ExampleDao,
) : ExampleRepository {

    override suspend fun insertExampleSentence(example: Example) {
        exampleDao.insertExampleSentence(example.asEntity())
    }

    override fun getExamplesByWord(word: String): Flow<List<Example>> {
        return exampleDao.getExamplesByWord(word).map {
            it.map { entity -> entity.asExternalModel() }
        }
    }

}

interface ExampleRepository {
    suspend fun insertExampleSentence(example: Example)
    fun getExamplesByWord(word: String): Flow<List<Example>>
}