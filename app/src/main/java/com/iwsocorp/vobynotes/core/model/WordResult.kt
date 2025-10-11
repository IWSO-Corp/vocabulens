package com.iwsocorp.vobynotes.core.model

data class WordResult(
    val word: String,
    val sourceLang: String,
    val translation: String,
    val targetLang: String,
)

fun WordResult.asCorpus(noteId: String): Corpus = Corpus(
    noteId = noteId,
    word = word,
    meaning = translation,
    wordLang = sourceLang,
    meaningLang = targetLang,
)