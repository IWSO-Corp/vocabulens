package com.iwsocorp.vocabnotes.core.data.repository

import com.iwsocorp.vocabnotes.core.database.dao.CorpusDao
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.model.Corpus
import com.iwsocorp.vocabnotes.core.model.asEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class CorpusRepositoryImpl @Inject constructor(
    private val corpusDao: CorpusDao,
    private val meaningRepository: MeaningRepository,
) : CorpusRepository {

    private suspend fun Flow<List<CorpusEntity>>.asExternalModel(): Flow<List<Corpus>> =
        this.map { corpusEntities ->
            corpusEntities.map {
                Timber.d("corpusEntities: $corpusEntities")
                val wordMeanings = meaningRepository.getMeanings(it.id).first()
                Timber.d("wordMeanings: $wordMeanings")
                Corpus(
                    id = it.id,
                    noteId = it.noteId,
                    word = it.word,
                    meaning = it.meaning,
                    phonetic = it.phonetic,
                    audio = it.audio,
                    meanings = wordMeanings,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            }
        }

    override suspend fun addCorpus(corpus: Corpus) {
        corpusDao.insertCorpus(corpus.asEntity())
    }

    override suspend fun updateCorpus(corpus: Corpus) {
        corpusDao.updateCorpus(corpus.asEntity())
    }

    override suspend fun deleteCorpus(id: String) {
        corpusDao.deleteCorpusById(id)
    }

    override suspend fun getCorpusById(id: String): Corpus {
        val wordMeanings = meaningRepository.getMeanings(id).first()
        return corpusDao.getCorpusById(id).let {
            Corpus(
                id = it.id,
                noteId = it.noteId,
                word = it.word,
                meaning = it.meaning,
                phonetic = it.phonetic,
                audio = it.audio,
                meanings = wordMeanings,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    override suspend fun searchCorpus(query: String): Flow<List<Corpus>> {
        return corpusDao.searchCorpus(query).asExternalModel()
    }

    override suspend fun getAllCorpus(): Flow<List<Corpus>> {
        return corpusDao.getAllCorpus().asExternalModel()
    }

    override suspend fun getCorpusByNoteId(noteId: String): Flow<List<Corpus>> {
        return corpusDao.getCorpusByNoteId(noteId).asExternalModel()
    }

    override suspend fun deleteCorpusByNoteId(noteId: String) {
        corpusDao.deleteCorpusByNoteId(noteId)
    }

}

interface CorpusRepository {
    suspend fun addCorpus(corpus: Corpus)
    suspend fun updateCorpus(corpus: Corpus)
    suspend fun deleteCorpus(id: String)
    suspend fun getCorpusById(id: String): Corpus
    suspend fun searchCorpus(query: String): Flow<List<Corpus>>
    suspend fun getAllCorpus(): Flow<List<Corpus>>
    suspend fun getCorpusByNoteId(noteId: String): Flow<List<Corpus>>
    suspend fun deleteCorpusByNoteId(noteId: String)
}