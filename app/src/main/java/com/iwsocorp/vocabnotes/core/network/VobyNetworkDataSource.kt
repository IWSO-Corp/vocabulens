package com.iwsocorp.vocabnotes.core.network

import com.iwsocorp.vocabnotes.core.network.model.VocabularyResponseItem

interface VobyNetworkDataSource {
    suspend fun getVocabulary(word: String): List<VocabularyResponseItem>
}