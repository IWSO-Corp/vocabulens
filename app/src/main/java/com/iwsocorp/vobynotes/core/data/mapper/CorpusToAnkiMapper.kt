package com.iwsocorp.vobynotes.core.data.mapper

import com.iwsocorp.vobynotes.core.model.Corpus

object CorpusToAnkiMapper {

    fun mapToCards(corpus: Corpus): List<Pair<Map<String, String>, Set<String>>> {

        val tags = AnkiTagMapper.fromCorpus(corpus)

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

    fun Corpus.toAnkiFields(): Array<String> {
        val firstMeaning = meanings.takeIf { it.isNotEmpty() }?.first()
        val firstDefinition = firstMeaning?.definitions?.first()
        return arrayOf(
            word,
            phonetic.ifEmpty { "-" },
            meaning,
            firstDefinition?.example ?: "-",
            firstDefinition?.definition ?: "-",
            firstMeaning?.partOfSpeech ?: "-"
        )
    }

}
