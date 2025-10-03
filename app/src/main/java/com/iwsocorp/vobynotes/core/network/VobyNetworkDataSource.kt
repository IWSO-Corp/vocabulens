package com.iwsocorp.vobynotes.core.network

import com.iwsocorp.vobynotes.core.network.model.VocabularyResponseItem

interface VobyNetworkDataSource {
    suspend fun getVocabulary(word: String): List<VocabularyResponseItem>
}