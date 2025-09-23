package com.iwsocorp.vocabnotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.dao.InsertResult
import com.iwsocorp.vocabnotes.core.database.dao.NoteDao
import com.iwsocorp.vocabnotes.core.database.dao.insertCorpusListWithResult
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.database.model.asExternalModel
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CorpusRepositoryImpl @Inject constructor(
    private val corpusDao: CorpusDao,
    private val noteDao: NoteDao
) : CorpusRepository {

    override suspend fun addCorpus(corpus: Corpus): Long {
        return corpusDao.insertCorpus(corpus.asEntity())
    }

    override suspend fun insertCorpusList(corpusList: List<Corpus>): InsertResult {
        return corpusDao.insertCorpusListWithResult(corpusList.map { it.asEntity() })
    }

    override suspend fun updateCorpus(corpus: Corpus) {
        corpusDao.updateCorpus(corpus.asEntity())
    }

    override suspend fun deleteCorpus(word: String) {
        corpusDao.deleteCorpusById(word)
    }

    override suspend fun getCorpusByWord(word: String): Corpus? {
        return corpusDao.getCorpusByWord(word)?.asExternalModel()
    }

    override fun searchCorpus(query: String): Flow<PagingData<Corpus>> {
        return createPager {
            corpusDao.searchCorpus(query)
        }
    }

    override fun getAllCorpus(): Flow<PagingData<Corpus>> {
        return createPager {
            corpusDao.getAllCorpus()
        }
    }

    override fun allCorpusSize(): Flow<Int> {
        return corpusDao.allCorpusSize()
    }

    override fun getLatestCorpus(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getLatestCorpus(noteId).map { list ->
            list.map { entity ->
                entity.asExternalModel()
            }
        }
    }

    override fun getCorpusByNoteId(noteId: String): Flow<PagingData<Corpus>> {
        return createPager {
            corpusDao.getCorpusByNoteId(noteId)
        }
    }

    override suspend fun deleteCorpusByNoteId(noteId: String) {
        corpusDao.deleteCorpusByNoteId(noteId)
    }

    override suspend fun countExisting(words: List<String>): Int {
        return corpusDao.countExisting(words)
    }

    private fun createPager(
        factory: () -> PagingSource<Int, CorpusEntity>,
    ): Flow<PagingData<Corpus>> {
        val pager: Pager<Int, CorpusEntity> = Pager(
            config = PagingConfig(
                pageSize = Int.MAX_VALUE,
                initialLoadSize = Int.MAX_VALUE
            ),
            pagingSourceFactory = factory
        )
        return pager.flow.map { pagingData ->
            pagingData.map { entity ->
                entity.asExternalModel()
            }
        }
    }
}

interface CorpusRepository {
    suspend fun addCorpus(corpus: Corpus): Long
    suspend fun insertCorpusList(corpusList: List<Corpus>): InsertResult
    suspend fun updateCorpus(corpus: Corpus)
    suspend fun deleteCorpus(word: String)
    suspend fun getCorpusByWord(word: String): Corpus?
    fun searchCorpus(query: String): Flow<PagingData<Corpus>>
    fun getAllCorpus(): Flow<PagingData<Corpus>>
    fun allCorpusSize(): Flow<Int>
    fun getLatestCorpus(noteId: String): Flow<List<Corpus>>
    fun getCorpusByNoteId(noteId: String): Flow<PagingData<Corpus>>
    suspend fun deleteCorpusByNoteId(noteId: String)
    suspend fun countExisting(words: List<String>): Int
}