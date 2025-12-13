package com.iwsocorp.vobynotes.core.data.mapper

import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark

object AnkiTagMapper {

    fun fromCorpus(corpus: Corpus): Set<String> {
        val tags = mutableSetOf<String>()

        tags += "vocabulens"
        tags += corpus.wordLang.lowercase()
        tags += corpus.meaningLang.lowercase()

        // mark → tag
        tags += when (corpus.mark.name) {
            Mark.FAMILIAR.name -> "familiar"
            Mark.UNFAMILIAR.name -> "unfamiliar"
            else -> "unmarked"
        }

        return tags
    }
}