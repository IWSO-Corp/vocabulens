package com.iwsocorp.vocabnotes.core.model

import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity

data class Corpus(
    val id: String,
    var noteId: String,
    val word: String,
    val meaning: String,
    var phonetic: String,
    var audio: String,
    var meanings: List<Meaning>,
    val createdAt: Long,
    val updatedAt: Long
)

fun Corpus.asEntity() = CorpusEntity(
    id = id,
    noteId = noteId,
    word = word,
    meaning = meaning,
    phonetic = phonetic,
    audio = audio,
    createdAt = createdAt,
    updatedAt = updatedAt
)