package com.iwsocorp.vobynotes.core.data.anki

/**
 * Konfigurasi integrasi AnkiDroid untuk aplikasi Vocabulens
 * Seluruh konstanta di sini bersifat statis dan immutable
 */
object AnkiDroidConfig {

    /** Nama deck yang akan dibuat / digunakan di AnkiDroid */
    const val DECK_NAME = "Vocabulens: Vocabulary"

    /** Nama model (note type) di AnkiDroid */
    const val MODEL_NAME = "Vocabulens"

    val FIELDS = arrayOf(
        "Word",
        "Reading",
        "Meaning",
        "PosNoun",
        "PosPronoun",
        "PosVerb",
        "PosAdjective",
        "PosAdverb",
        "PosPreposition",
        "PosConjunction",
        "PosInterjection"
    )

    val CARD_NAMES = arrayOf(
        "Vocabulary",
        "Noun",
        "Pronoun",
        "Verb",
        "Adjective",
        "Adverb",
        "Preposition",
        "Conjunction",
        "Interjection"
    )

    val QFMT = arrayOf(
        // Default
        """
        <div class="word">{{Word}}</div>
        <div class="reading">{{Reading}}</div>
        """,
        // Pos defined
        posQFormat("Noun"),
        posQFormat("Pronoun"),
        posQFormat("Verb"),
        posQFormat("Adjective"),
        posQFormat("Adverb"),
        posQFormat("Preposition"),
        posQFormat("Conjunction"),
        posQFormat("Interjection")
    )

    private fun posQFormat(pos: String): String = """
        {{#Pos$pos}}
        <div class='word'>{{Word}}</div>
        <div class="reading">{{Reading}}</div>
        <br>
        <small>($pos)</small>
        {{/Pos$pos}}
        """.trimIndent()

    val AFMT = arrayOf(
        // Default
        """
        <div class='word'>{{Word}}</div>
        <div class="reading">{{Reading}}</div>
        <br>
        <b>{{Meaning}}</b>
        <br><hr>
        <small>{{Tags}}</small>
        """,
        // Pos defined
        posAFormat("Noun"),
        posAFormat("Pronoun"),
        posAFormat("Verb"),
        posAFormat("Adjective"),
        posAFormat("Adverb"),
        posAFormat("Preposition"),
        posAFormat("Conjunction"),
        posAFormat("Interjection")
    )

    private fun posAFormat(pos: String): String = """
        {{#Pos$pos}}
        <div class='word'>{{Word}}</div>
        <div class="reading">{{Reading}}</div>
        <br>
        <b>{{Meaning}}</b>
        <br><hr>
        <small>${pos.lowercase()}</small>
        <br>
        <div class='pos'>{{Pos$pos}}</div>
        <hr>
        <small>{{Tags}}</small>
        {{/Pos$pos}}
        """.trimIndent()

    /** CSS global untuk seluruh kartu */
    val CSS: String = """
        .card {
            font-family: NotoSans;
            font-size: 22px;
            text-align: center;
            color: #222222;
            background-color: #ffffff;
            word-wrap: break-word;
        }

        .word {
            font-size: 40px;
            font-weight: bold;
        }

        .example {
            font-size: 18px;
            color: #555555;
            margin-top: 16px;
        }
    """.trimIndent()

}
