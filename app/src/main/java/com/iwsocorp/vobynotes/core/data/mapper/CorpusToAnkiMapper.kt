package com.iwsocorp.vobynotes.core.data.mapper

import com.iwsocorp.vobynotes.core.data.anki.AnkiDroidConfig
import com.iwsocorp.vobynotes.core.model.Corpus
import com.iwsocorp.vobynotes.core.model.Mark
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    fun generateFlashcards(corpus: Corpus): List<Pair<Map<String, String>, Set<String>>> {
        val tags = generateTags(corpus)

        // Fallback: jika meanings kosong, buat 1 card dasar
        if (corpus.meanings.isEmpty()) {
            val fields = mapOf(
                "Word" to corpus.word,
                "Reading" to corpus.phonetic.ifBlank { "-" },
                "Meaning" to corpus.meaning,
                "ExampleSentence" to "-",
                "ExampleMeaning" to "-",
                "PartOfSpeech" to "-"
            )

            return listOf(fields to tags)
        }

        // Normal case
        return corpus.meanings.map { meaning ->
            val definition = meaning.definitions.firstOrNull()
            val fields = mapOf(
                "Word" to corpus.word,
                "Reading" to corpus.phonetic.ifBlank { "-" },
                "Meaning" to corpus.meaning,
                "ExampleSentence" to definition?.example.orEmpty(),
                "ExampleMeaning" to definition?.definition.orEmpty(),
                "PartOfSpeech" to meaning.partOfSpeech.ifBlank { "-" }
            )

            fields to tags
        }
    }

    fun generateAnkiFields(corpus: Corpus): List<Array<String>> {
        val meaning = corpus.meanings

        return if (meaning.isEmpty()) listOf(
            arrayOf(
                corpus.word,
                corpus.phonetic.ifBlank { "-" },
                corpus.meaning,
                "-",
                "-",
                "-"
            )
        ) else meaning.map {
            val definition = it.definitions.firstOrNull()
            val pos = it.partOfSpeech

            arrayOf(
                "${corpus.word} ($pos)",
                corpus.phonetic.ifBlank { "-" },
                corpus.meaning,
                definition?.example ?: "-",
                definition?.definition ?: "-",
                pos
            )
        }
    }

    fun makeDeckName(title: String, wordLang: String, meaningLang: String): String {
        val safeTitle = "$title ($wordLang - $meaningLang)"
            .trim()
            .replace("::", "-")
            .replace("/", "-")

        return "${AnkiDroidConfig.DECK_NAME} :: $safeTitle"
    }

    fun Corpus.toAnkiFields(): Array<String> {
        return arrayOf(
            word,
            phonetic.ifBlank { "-" },
            meaning,
            toPosJson()
        )
    }

    fun Corpus.toPosJson(): String {
        val map = meanings
            .groupBy { it.partOfSpeech.lowercase() }
            .mapValues { (_, meanings) ->
                meanings.flatMap { m ->
                    m.definitions.map { def ->
                        PosEntry(def.definition, def.example)
                    }
                }
            }

        return Json.encodeToString(PosPayload(map))
    }

}

@Serializable
data class PosEntry(
    val definition: String,
    val example: String? = null
)

@Serializable
data class PosPayload(
    val pos: Map<String, List<PosEntry>>
)