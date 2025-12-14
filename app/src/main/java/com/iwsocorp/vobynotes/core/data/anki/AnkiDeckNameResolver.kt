package com.iwsocorp.vobynotes.core.data.anki

import com.iwsocorp.vobynotes.core.model.Note

object AnkiDeckNameResolver {

    fun fromNote(note: Note): String {
        val safeTitle = "${note.title} (${note.wordLang} - ${note.meaningLang})"
            .trim()
            .replace("::", "-")
            .replace("/", "-")

        return "${AnkiDroidConfig.DECK_NAME} :: $safeTitle"
    }
}