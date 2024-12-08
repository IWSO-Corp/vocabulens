package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CorpusRepositoryImpl @Inject constructor(
    private val corpusDao: CorpusDao,
) : CorpusRepository {

    private fun Flow<List<CorpusEntity>>.asExternalModelList(): Flow<List<Corpus>> = flow {
        val corpusBatch = mutableListOf<Corpus>()

        this@asExternalModelList.collect { corpusEntities ->
            for (corpusEntity in corpusEntities) {
                // Convert the entity to its external model representation
                val corpus = corpusEntity.asExternalModel()

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
        corpusDao.insertCorpusList(corpusList.map { it.asEntity() })
    }

    override suspend fun updateCorpus(corpus: Corpus) {
        corpusDao.updateCorpus(corpus.asEntity())
    }

    override suspend fun deleteCorpus(id: String) {
        corpusDao.deleteCorpusById(id)
    }

    override suspend fun getCorpusByWord(word: String): Corpus {
        return corpusDao.getCorpusByWord(word).asExternalModel()
    }

    override fun searchCorpus(query: String): Flow<List<Corpus>> {
        return corpusDao.searchCorpus(query).asExternalModelList()
    }

    override fun getAllCorpus(): Flow<List<Corpus>> {
        return corpusDao.getAllCorpus().asExternalModelList()
    }

    override fun getCorpusByNoteId(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getCorpusByNoteId(noteId).asExternalModelList()
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