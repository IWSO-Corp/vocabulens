package com.iwsocorp.vobynotes.core.common

/**
 * Konfigurasi integrasi AnkiDroid untuk aplikasi Vocabulens
 * Seluruh konstanta di sini bersifat statis dan immutable
 */
object AnkiDroidConfig {

    /** Nama deck yang akan dibuat / digunakan di AnkiDroid */
    const val DECK_NAME = "Vocabulens Vocabulary"

    /** Nama model (note type) di AnkiDroid */
    const val MODEL_NAME = "com.iwsocorp.vobynotes.model"

    /** Tag default untuk setiap note */
    val TAGS: Set<String> = setOf(
        "vocabulens",
        "auto_generated"
    )

    /** Field yang digunakan pada model Anki */
    val FIELDS = arrayOf(
        "Word",
        "Reading",
        "Meaning",
        "ExampleSentence",
        "ExampleMeaning",
        "PartOfSpeech"
    )

    /** Nama kartu (card templates) */
    val CARD_NAMES = arrayOf(
        "Word → Meaning",
        "Meaning → Word"
    )

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

    /** Question format untuk masing-masing kartu */
    val QFMT = arrayOf(
        // Word → Meaning
        "<div class='word'>{{Word}}</div><br>{{Reading}}<br><small>{{PartOfSpeech}}</small>",

        // Meaning → Word
        "<div class='word'>{{Meaning}}</div><br><small>{{PartOfSpeech}}</small>"
    )

    /** Answer format (sama untuk kedua kartu) */
    val AFMT = arrayOf(
        """
        <div class='word'>{{Word}}</div>
        <br>{{Reading}}
        <hr>
        <b>{{Meaning}}</b>
        <div class='example'>
            {{ExampleSentence}}<br>
            <i>{{ExampleMeaning}}</i>
        </div>
        <br>
        <small>{{Tags}}</small>
        """.trimIndent(),

        """
        <div class='word'>{{Word}}</div>
        <br>{{Reading}}
        <hr>
        <b>{{Meaning}}</b>
        <div class='example'>
            {{ExampleSentence}}<br>
            <i>{{ExampleMeaning}}</i>
        </div>
        <br>
        <small>{{Tags}}</small>
        """.trimIndent()
    )

    /** Field utama (untuk legacy ACTION_SEND jika diperlukan) */
    const val FRONT_SIDE_KEY = "Word"
    const val BACK_SIDE_KEY = "Meaning"

    /**
     * Contoh data dummy untuk testing integrasi
     */
    fun getSampleData(): List<Map<String, String>> {
        val words = listOf("Resilient", "Persist", "Insight")
        val readings = listOf("-", "-", "-")
        val meanings = listOf(
            "Mampu bangkit kembali",
            "Terus bertahan atau melanjutkan",
            "Pemahaman yang mendalam"
        )
        val examples = listOf(
            "She remained resilient despite many failures.",
            "He decided to persist until he succeeded.",
            "This experience gave him valuable insight."
        )
        val exampleMeanings = listOf(
            "Dia tetap tangguh meskipun banyak kegagalan.",
            "Dia memutuskan untuk bertahan sampai berhasil.",
            "Pengalaman ini memberinya pemahaman berharga."
        )
        val pos = listOf("Adjective", "Verb", "Noun")

        return words.indices.map { idx ->
            mapOf(
                FIELDS[0] to words[idx],
                FIELDS[1] to readings[idx],
                FIELDS[2] to meanings[idx],
                FIELDS[3] to examples[idx],
                FIELDS[4] to exampleMeanings[idx],
                FIELDS[5] to pos[idx]
            )
        }
    }
}
