package com.iwsocorp.vocabnotes.core.model

import com.google.gson.Gson
import com.iwsocorp.vocabnotes.core.database.model.CorpusEntity
import com.iwsocorp.vocabnotes.core.database.model.Mark

data class Corpus(
    val word: String,
    val noteId: String,
    val meaning: String,
    val wordLang: String,
    val meaningLang: String,
    val phonetic: String = "",
    val audio: String = "",
    val meanings: List<Meaning> = emptyList(),
    val mark: Mark = Mark.UNMARKED,
    val createdAt: Long,
    val updatedAt: Long
)

fun Corpus.asEntity() : CorpusEntity {
    val meaningsJson = Gson().toJson(meanings)
    return CorpusEntity(
        word = word,
        noteId = noteId,
        meaning = meaning,
        wordLang = wordLang,
        meaningLang = meaningLang,
        phonetic = phonetic,
        audio = audio,
        meaningsJson = meaningsJson,
        mark = mark,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}