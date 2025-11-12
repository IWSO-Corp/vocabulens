package com.iwsocorp.vobynotes.core.model

data class Vocabulary(
    val word: String,
    val phonetic: String,
    val audio: String,
    val meanings: List<Meaning>,
)

data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition>,
    val synonyms: List<String>,
    val antonyms: List<String>,
)

data class Definition(
    val definition: String,
    val example: String?,
)

fun Vocabulary.toCorpus(meaningLang: String, meaning: String): Corpus = Corpus(
    word = word,
    phonetic = phonetic,
    audio = audio,
    meanings = meanings,
    noteId = "",
    wordLang = "en",
    meaningLang = meaningLang,
    meaning = meaning,
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
)