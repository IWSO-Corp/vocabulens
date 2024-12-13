package com.iwsocorp.vocabnotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CorpusRepositoryImpl @Inject constructor(
    private val corpusDao: CorpusDao,
) : CorpusRepository {

    private fun Flow<List<CorpusEntity>>.asExternalModelList(): Flow<List<Corpus>> {
        return this.map {
            it.map { entity ->
                entity.asExternalModel()
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

    override fun getLatestCorpus(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getLatestCorpus(noteId).asExternalModelList()
    }

    override fun getCorpusByNoteId(noteId: String): Flow<PagingData<Corpus>> {
        val pager: Pager<Int, CorpusEntity> = Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                corpusDao.getCorpusByNoteId(noteId)
            }
        )
        return pager.flow.map { pagingData ->
            pagingData.map { entity ->
                entity.asExternalModel()
            }
        }
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
    fun getLatestCorpus(noteId: String): Flow<List<Corpus>>
    fun getCorpusByNoteId(noteId: String): Flow<PagingData<Corpus>>
    suspend fun deleteCorpusByNoteId(noteId: String)
}