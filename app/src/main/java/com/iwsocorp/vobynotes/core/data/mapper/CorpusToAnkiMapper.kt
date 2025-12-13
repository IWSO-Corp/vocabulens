package com.iwsocorp.vobynotes.core.data.mapper

import com.iwsocorp.vobynotes.core.model.Corpus

object CorpusToAnkiMapper {

    fun mapToCards(corpus: Corpus): List<Pair<Map<String, String>, Set<String>>> {

        return corpus.meanings.map { meaning ->

            val fields = mapOf(
                "Word" to corpus.word,
                "Reading" to corpus.phonetic.ifBlank { "-" },
                "Meaning" to corpus.meaning,
                "ExampleSentence" to meaning.definitions.first().example.orEmpty(),
                "ExampleMeaning" to meaning.definitions.first().definition,
                "PartOfSpeech" to meaning.partOfSpeech,
            )

            val tags = AnkiTagMapper.fromCorpus(corpus)

            fields to tags
        }
    }

    fun Corpus.toAnkiFields(): Array<String> {
        val firstMeaning = meanings.takeIf { it.isNotEmpty() }?.first()
        val firstDefinition = firstMeaning?.definitions?.first()
        return arrayOf(
            word,
            phonetic,
            meaning,
            firstDefinition?.example.orEmpty(),
            firstDefinition?.definition.orEmpty(),
            firstMeaning?.partOfSpeech.orEmpty()
        )
    }

}
