package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class CorpusRepositoryImpl @Inject constructor(
    private val corpusDao: CorpusDao,
    private val meaningRepository: MeaningRepository,
) : CorpusRepository {

    private fun Flow<List<CorpusEntity>>.asExternalModel(): Flow<List<Corpus>> = flow {
        val corpusBatch = mutableListOf<Corpus>()

        this@asExternalModel.collect { corpusEntities ->
            for (corpusEntity in corpusEntities) {
                // Fetch word meanings (this is the potentially long operation)
                val wordMeanings = meaningRepository.getMeanings(corpusEntity.word).first()

                // Convert the entity to its external model representation
                val corpus = corpusEntity.asExternalModel(wordMeanings)

                // Add the result to the batch
                corpusBatch.add(corpus)

                // Emit the batch every 10 items
                if (corpusBatch.size == 10) {
                    emit(corpusBatch.toList())  // Emit the current batch
                    corpusBatch.clear()         // Clear the batch for the next set of items
                }
            }

            // Emit any remaining items (less than 10)
            if (corpusBatch.isNotEmpty()) {
                emit(corpusBatch.toList())
            }
        }
    }

    override suspend fun addCorpus(corpus: Corpus) {
        corpusDao.insertCorpus(corpus.asEntity())
    }

    override suspend fun insertCorpusList(corpusList: List<Corpus>) {
        Timber.d("imported data: ${corpusList.take(5)}")
        Timber.d("imported data size: ${corpusList.size}")
        corpusDao.insertCorpusList(corpusList.map { it.asEntity() })
    }

    override suspend fun updateCorpus(corpus: Corpus) {
        corpusDao.updateCorpus(corpus.asEntity())
    }

    override suspend fun deleteCorpus(id: String) {
        corpusDao.deleteCorpusById(id)
    }

    override suspend fun getCorpusByWord(word: String): Corpus {
        val wordMeanings = meaningRepository.getMeanings(word).first()
        return corpusDao.getCorpusByWord(word).asExternalModel(wordMeanings)
    }

    override fun searchCorpus(query: String): Flow<List<Corpus>> {
        return corpusDao.searchCorpus(query).asExternalModel()
    }

    override fun getAllCorpus(): Flow<List<Corpus>> {
        return corpusDao.getAllCorpus().asExternalModel()
    }

    override fun getCorpusByNoteId(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getCorpusByNoteId(noteId).asExternalModel()
    }

    override suspend fun deleteCorpusByNoteId(noteId: String) {
        corpusDao.deleteCorpusByNoteId(noteId)
    }

}

interface CorpusRepository {
    suspend fun addCorpus(corpus: Corpus)
    suspend fun insertCorpusList(corpusList: List<Corpus>)
    suspend fun updateCorpus(corpus: Corpus)
    suspend fun deleteCorpus(id: String)
    suspend fun getCorpusByWord(word: String): Corpus
    fun searchCorpus(query: String): Flow<List<Corpus>>
    fun getAllCorpus(): Flow<List<Corpus>>
    fun getCorpusByNoteId(noteId: String): Flow<List<Corpus>>
    suspend fun deleteCorpusByNoteId(noteId: String)
}