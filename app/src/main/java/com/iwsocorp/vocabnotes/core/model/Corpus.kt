package com.iwsocorp.vocabnotes.core.model

import com.google.gson.Gson
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity

data class Corpus(
    val id: String,
    val noteId: String,
    val word: String,
    val meaning: String,
    val wordLang: String,
    val meaningLang: String,
    val phonetic: String = "",
    val audio: String = "",
    val meanings: List<Meaning> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

fun Corpus.asEntity() : CorpusEntity {
    val meaningsJson = Gson().toJson(meanings)
    return CorpusEntity(
        id = id,
        noteId = noteId,
        word = word,
        meaning = meaning,
        wordLang = wordLang,
        meaningLang = meaningLang,
        phonetic = phonetic,
        audio = audio,
        meaningsJson = meaningsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}