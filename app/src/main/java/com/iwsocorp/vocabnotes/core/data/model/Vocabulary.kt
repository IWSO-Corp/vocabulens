package com.iwsocorp.vocabnotes.core.data.model

import com.iwsocorp.vocabnotes.core.model.Definition
import com.iwsocorp.vocabnotes.core.model.Meaning
import com.iwsocorp.vocabnotes.core.model.Vocabulary
import com.iwsocorp.vocabnotes.core.network.model.DefinitionsItem
import com.iwsocorp.vocabnotes.core.network.model.MeaningsItem
import com.iwsocorp.vocabnotes.core.network.model.PhoneticsItem
import com.iwsocorp.vocabnotes.core.network.model.VocabularyResponseItem

fun VocabularyResponseItem.asVocabulary(): Vocabulary {
    val phonetic: String = phonetic ?: phonetics?.takeIf { it.isNotEmpty() }?.first()?.text ?: "-"
    val phoneticsWithAudio: List<PhoneticsItem?>? =
        phonetics?.filter { !it?.audio.isNullOrEmpty() }.takeIf { it?.isNotEmpty() == true }
    val audio: String = phoneticsWithAudio?.first()?.audio ?: ""
    val meanings: List<Meaning> = meanings?.map { it!!.asMeaning() } ?: listOf()

    return Vocabulary(
        word = word ?: "",
        phonetic = phonetic,
        audio = audio,
        meanings = meanings
    )
}

fun MeaningsItem.asMeaning(): Meaning {
    val defWithEx: List<DefinitionsItem?>? =
        this.definitions!!.filter { !it!!.example.isNullOrEmpty() }.takeIf { it.isNotEmpty() }
    val definition = defWithEx?.first()?.definition ?: this.definitions.first()?.definition ?: ""
    val example = defWithEx?.first()?.example ?: ""
    val synonyms = this.synonyms?.map { it.toString() } ?: listOf()
    val antonyms = this.antonyms?.map { it.toString() } ?: listOf()

    return Meaning(
        partOfSpeech = partOfSpeech ?: "",
        definitions = listOf(Definition(definition, example)),
        synonyms = synonyms,
        antonyms = antonyms
    )
}