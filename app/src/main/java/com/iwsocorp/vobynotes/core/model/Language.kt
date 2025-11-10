package com.iwsocorp.vobynotes.core.model

data class Language(
    val code: String,
    val name: String
)

data class SupportedLanguages(
    val supported_languages: List<Language>
)