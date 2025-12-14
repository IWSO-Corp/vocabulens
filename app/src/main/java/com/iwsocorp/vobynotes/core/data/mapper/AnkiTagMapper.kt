package com.iwsocorp.vobynotes.core.data.mapper

import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark

object AnkiTagMapper {

    fun fromCorpus(corpus: Corpus): Set<String> {
        val tags = mutableSetOf<String>()

        tags += if (corpus.meanings.isEmpty()) "undefined" else "defined"

        // mark → tag
        tags += when (corpus.mark.name) {
            Mark.FAMILIAR.name -> "familiar"
            Mark.UNFAMILIAR.name -> "unfamiliar"
            else -> "unmarked"
        }

        return tags
    }
}