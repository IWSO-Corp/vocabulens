package com.iwsocorp.vocabnotes.data.model

data class Corpus(
    val id: String,
    val vocabularyId: String,
    val word: String,
    val meaning: String,
    var phonetic: String,
    var audio: String,
    var meanings: List<Meanings>,
    val createdAt: Long,
    val updatedAt: Long
)
