package com.iwsocorp.vobynotes.core.model

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
    return CorpusEntity(
        id = id,
        word = word,
        noteId = noteId,
        meaning = meaning,
        wordLang = wordLang,
        meaningLang = meaningLang,
        phonetic = phonetic,
        audio = audio,
        meanings = meanings,
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