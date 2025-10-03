package com.iwsocorp.vobynotes.core.model

import com.google.gson.Gson
import com.iwsocorp.vobynotes.core.database.model.CorpusEntity
import java.util.UUID

data class Corpus(
    val noteId: String,
    val word: String,
    val meaning: String,
    val wordLang: String,
    val meaningLang: String,
    val id: String = UUID.randomUUID().toString(),
    val phonetic: String = "",
    val audio: String = "",
    val meanings: List<Meaning> = emptyList(),
    val mark: Mark = Mark.UNMARKED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val indexNumber: Int = 0,
)

fun Corpus.asEntity(): CorpusEntity {
    val meaningsJson = Gson().toJson(meanings)
    return CorpusEntity(
        id = id,
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

enum class Mark {
    UNMARKED,
    FAMILIAR,
    UNFAMILIAR
}