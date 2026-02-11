package com.iwsocorp.vobynotes.core.data.mapper

import com.iwsocorp.vobynotes.core.data.anki.AnkiDroidConfig
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import com.iwsocorp.vobynotes.core.model.Meaning

object CorpusToAnkiMapper {

    fun generateTags(corpus: Corpus, pos: String? = null): Set<String> {
        val tags = mutableSetOf<String>()

        tags += if (corpus.meanings.isEmpty()) "undefined" else "defined"

        // mark → tag
        tags += when (corpus.mark.name) {
            Mark.FAMILIAR.name -> "familiar"
            Mark.UNFAMILIAR.name -> "unfamiliar"
            else -> "unmarked"
        }

        pos?.let {
            tags += it.lowercase()
        }

        return tags
    }

    fun makeDeckName(title: String, wordLang: String, meaningLang: String): String {
        val safeTitle = "$title ($wordLang - $meaningLang)"
            .trim()
            .replace("::", "-")
            .replace("/", "-")

        return "${AnkiDroidConfig.DECK_NAME} :: $safeTitle"
    }

    fun Corpus.toAnkiFields(): Array<String> {
        val posHtml = fromMeanings(this.meanings)

        return arrayOf(
            this.word,
            this.phonetic.ifBlank { "-" },
            this.meaning,
            posHtml["noun"] ?: "",
            posHtml["verb"] ?: "",
            posHtml["adjective"] ?: "",
            posHtml["adverb"] ?: "",
            posHtml["pronoun"] ?: "",
            posHtml["preposition"] ?: "",
            posHtml["conjunction"] ?: "",
            posHtml["interjection"] ?: "",
        )
    }

    fun fromMeanings(meanings: List<Meaning>): Map<String, String> {
        return meanings
            .groupBy { it.partOfSpeech.lowercase() }
            .mapValues { (_, list) ->
                buildString {
                    list.forEach { m ->
                        m.definitions.forEach {
                            append("<p><b>${it.definition}</b>")
                            if (it.definition.isNotBlank()) append("<br><br><i>${it.example}</i>")
                            append("</p>")
                        }
                    }
                }
            }
    }

}