package com.iwsocorp.vocabnotes.core.model

data class Note(
    val id: String,
    val title: String,
    val wordLang: String,
    val meaningLang: String,
    val content: List<Corpus>,
    val createdAt: Long,
    val updatedAt: Long
)
