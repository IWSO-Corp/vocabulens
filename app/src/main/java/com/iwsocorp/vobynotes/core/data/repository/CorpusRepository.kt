package com.iwsocorp.vobynotes.core.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import com.iwsocorp.vobynotes.core.database.dao.CorpusDao
import com.iwsocorp.vobynotes.core.database.dao.InsertResult
import com.iwsocorp.vobynotes.core.database.dao.insertCorpusListWithResult
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import com.iwsocorp.vobynotes.core.database.model.asExternalModel
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Example
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.SortBy
import com.iwsocorp.vobynotes.core.model.SortOrder
import com.iwsocorp.vobynotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CorpusRepositoryImpl @Inject constructor(
    private val corpusDao: CorpusDao,
) : CorpusRepository {

    override suspend fun addCorpus(corpus: Corpus): Long {
        return corpusDao.insertCorpus(corpus.asEntity())
    }

    override suspend fun insertCorpusList(corpusList: List<Corpus>): InsertResult {
        return corpusDao.insertCorpusListWithResult(corpusList.map { it.asEntity() })
    }

    override suspend fun updateCorpus(corpus: Corpus) {
        val examples = mutableListOf<Example>()

        corpus.meanings.map { meaning ->
            meaning.definitions.map { definition ->
                definition.example?.let {
                    if (it.isNotEmpty()) examples.add(Example(corpus.id, corpus.word, it))
                }
            }
        }

        corpusDao.updateCorpusAndInsertExamples(
            corpus.asEntity(),
            examples.toList().map {
                it.asEntity()
            }
        )
    }

    override suspend fun deleteBatch(ids: List<String>) {
        corpusDao.deleteBatch(ids)
    }

    override suspend fun getCorpusByWord(word: String): Corpus? {
        return corpusDao.getCorpusByWord(word)?.asExternalModel()
    }

    override fun getCorpusById(id: String): Flow<Corpus> {
        return corpusDao.getCorpusById(id).map { it.asExternalModel() }
    }

    override fun searchCorpus(query: String): Flow<PagingData<Corpus>> {
        return createPager { corpusDao.searchCorpus(query) }
    }

    override fun getAllCorpus(): Flow<PagingData<Corpus>> {
        return createPager { corpusDao.getAllCorpus() }
    }

    override fun getPagedCorpus(
        noteId: String,
        mark: Mark?,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): Flow<PagingData<Corpus>> = createPager {
        corpusDao.getPagedCorpus(noteId, mark, sortBy.column, sortOrder.value)
    }

    override fun allCorpusFlow(): Flow<List<Corpus>> {
        return corpusDao.allCorpusFlow().map { list -> list.map { it.asExternalModel() } }
    }

    override fun getCorpusByNoteId(noteId: String): Flow<PagingData<Corpus>> = createPager {
        corpusDao.getCorpusByNoteId(noteId)
    }

    override suspend fun deleteCorpusByNoteId(noteId: String) {
        corpusDao.deleteCorpusByNoteId(noteId)
    }

    override suspend fun countExisting(words: List<String>): Int {
        return corpusDao.countExisting(words)
    }

    override suspend fun moveCorpusToNote(
        corpusIds: List<String>,
        newNoteId: String,
    ) {
        corpusDao.moveCorpusToNote(corpusIds, newNoteId)
    }

    override suspend fun updateCorpusMark(
        corpusIds: List<String>,
        newMark: Mark,
    ) {
        corpusDao.updateCorpusMark(corpusIds, newMark)
    }

    private fun createPager(
        factory: () -> PagingSource<Int, CorpusEntity>,
    ): Flow<PagingData<Corpus>> = Pager(
        config = PagingConfig(
            pageSize = Int.MAX_VALUE,
            initialLoadSize = Int.MAX_VALUE
        ),
        pagingSourceFactory = factory
    ).flow.map { pagingData ->
        pagingData.map { entity ->
            entity.asExternalModel()
        }
    }

}

interface CorpusRepository {
    suspend fun addCorpus(corpus: Corpus): Long
    suspend fun insertCorpusList(corpusList: List<Corpus>): InsertResult
    suspend fun updateCorpus(corpus: Corpus)
    suspend fun deleteBatch(ids: List<String>)
    suspend fun getCorpusByWord(word: String): Corpus?
    fun getCorpusById(id: String): Flow<Corpus>
    fun searchCorpus(query: String): Flow<PagingData<Corpus>>
    fun getAllCorpus(): Flow<PagingData<Corpus>>
    fun getPagedCorpus(
        noteId: String,
        mark: Mark?,
        sortBy: SortBy,
        sortOrder: SortOrder,
    ): Flow<PagingData<Corpus>>

    fun allCorpusFlow(): Flow<List<Corpus>>
    fun getCorpusByNoteId(noteId: String): Flow<PagingData<Corpus>>
    suspend fun deleteCorpusByNoteId(noteId: String)
    suspend fun countExisting(words: List<String>): Int
    suspend fun moveCorpusToNote(corpusIds: List<String>, newNoteId: String)
    suspend fun updateCorpusMark(corpusIds: List<String>, newMark: Mark)
}