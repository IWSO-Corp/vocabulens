package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.data.model.asEntity
import com.iwsocorp.vocabnotes.core.database.dao.MeaningDao
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Meaning
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class MeaningRepositoryImpl @Inject constructor(
    private val meaningDao: MeaningDao,
) : MeaningRepository {

    override suspend fun insertMeanings(wordId: String, meanings: List<Meaning>) {
        Timber.d("meaningsData: $meanings")
        meaningDao.insertWordMeanings(meanings.map { it.asEntity(wordId) })
    }

    override fun getMeanings(wordId: String): Flow<List<Meaning>> {
        return meaningDao.getWordMeanings(wordId).map { meaningEntities ->
            meaningEntities.map { it.asExternalModel() }
        }
    }

}

interface MeaningRepository {
    suspend fun insertMeanings(wordId: String, meanings: List<Meaning>)
    fun getMeanings(wordId: String): Flow<List<Meaning>>
}