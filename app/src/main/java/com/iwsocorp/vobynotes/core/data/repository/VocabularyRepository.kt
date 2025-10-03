package com.iwsocorp.vobynotes.core.data.repository

import com.iwsocorp.vobynotes.core.data.model.asVocabulary
import com.iwsocorp.vobynotes.core.model.Vocabulary
import com.iwsocorp.vobynotes.core.network.VobyNetworkDataSource
import timber.log.Timber
import javax.inject.Inject

class VocabularyRepositoryImpl @Inject constructor(
    private val networkDataSource: VobyNetworkDataSource,
) : VocabularyRepository {

    override suspend fun getVocabulary(word: String): Vocabulary {
        Timber.d("getVocabularyItem: ${networkDataSource.getVocabulary(word)}")
        return networkDataSource.getVocabulary(word).first().asVocabulary()
    }

}

interface VocabularyRepository {
    suspend fun getVocabulary(word: String): Vocabulary
}