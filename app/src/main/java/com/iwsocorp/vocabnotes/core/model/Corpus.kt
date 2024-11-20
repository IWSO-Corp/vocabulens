package com.iwsocorp.vocabnotes.core.model

data class Corpus(
    val id: String,
    var noteId: String,
    val word: String,
    val meaning: String,
    var phonetic: String,
    var audio: String,
    var meanings: List<Meanings>,
    val createdAt: Long,
    val updatedAt: Long
)
