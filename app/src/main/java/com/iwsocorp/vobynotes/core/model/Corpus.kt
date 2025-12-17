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
    val ankiNoteId: Long? = null
)

fun Corpus.asEntity() = CorpusEntity(
    id = id,
    word = if (word.trim().uppercase() != "I") word.trim().lowercase() else word.trim().uppercase(),
    noteId = noteId,
    meaning = meaning.trim().lowercase(),
    wordLang = wordLang,
    meaningLang = meaningLang,
    phonetic = phonetic,
    audio = audio,
    meanings = meanings,
    hasMeaning = meanings.isNotEmpty(),
    mark = mark,
    createdAt = createdAt,
    updatedAt = updatedAt,
    ankiNoteId = ankiNoteId
)

enum class Mark {
    UNMARKED,
    FAMILIAR,
    UNFAMILIAR
}