package com.iwsocorp.vobynotes.core.model

import com.google.firebase.Timestamp

data class SharedNote(
    val id: String = "",
    val ownerId: String = "",
    val ownerAvatar: String? = null,
    val ownerName: String? = null,
    val title: String = "",
    val wordLang: String = "",
    val meaningLang: String = "",
    val content: List<SharedCorpus> = emptyList(),
    val savedCount: Int = 0,
    val uploadedAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
)

data class SharedCorpus(
    val noteId: String = "",
    val word: String = "",
    val meaning: String = "",
    val wordLang: String = "",
    val meaningLang: String = "",
    val examples: List<String> = emptyList(),
)

fun Note.asSharedNote(
    ownerId: String,
    ownerAvatar: String?,
    ownerName: String?,
    content: List<SharedCorpus>
) = SharedNote(
    id = id,
    ownerId = ownerId,
    ownerAvatar = ownerAvatar,
    ownerName = ownerName,
    title = title,
    wordLang = wordLang,
    meaningLang = meaningLang,
    content = content,
)

fun Corpus.asSharedCorpus(examples: List<String>) = SharedCorpus(
    noteId = noteId,
    word = word,
    meaning = meaning,
    wordLang = wordLang,
    meaningLang = meaningLang,
    examples = examples,
)

fun SharedNote.asNote() = Note(
    id = id,
    ownerId = ownerId,
    title = title,
    wordLang = wordLang,
    meaningLang = meaningLang,
    contentSize = content.size,
    shared = true,
)

fun SharedCorpus.asCorpus() = Corpus(
    noteId = noteId,
    word = word,
    meaning = meaning,
    wordLang = wordLang,
    meaningLang = meaningLang,
)