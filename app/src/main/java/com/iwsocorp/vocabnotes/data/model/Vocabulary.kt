package com.iwsocorp.vocabnotes.data.model

data class Vocabulary(
    val word: String,
    val phonetic: String,
    val audio: String,
    val meanings: List<Meanings>
)

data class Meanings(
    val partOfSpeech: String,
    val definitions: List<Definitions>,
    val synonyms: List<String>,
    val antonyms: List<String>
)

data class Definitions(
    val definition: String,
    val example: String
)