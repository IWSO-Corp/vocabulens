package com.iwsocorp.vobynotes.core.data.anki

import com.iwsocorp.vobynotes.core.model.Note

object AnkiDeckNameResolver {

    fun fromNote(note: Note): String {
        val safeTitle = note.title
            .trim()
            .replace("::", "-")
            .replace("/", "-")

        return "Vocabulens :: $safeTitle"
    }
}