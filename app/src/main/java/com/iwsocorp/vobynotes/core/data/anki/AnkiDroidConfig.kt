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
        "PosJson"
    )

    val CARD_NAMES = arrayOf("Vocabulary")

    val QFMT = arrayOf(
        """
        <div class="word">{{Word}}</div>
        <div class="reading">{{Reading}}</div>
        """
    )

    val AFMT = arrayOf(
        """
        <div class='word'>{{Word}}</div>
        <div class="reading">{{Reading}}</div>
        <br>
        <b>{{Meaning}}</b>
        <br><hr>
        <div id="pos"></div>
        <hr>
        <small>{{Tags}}</small>
    
        <script type="text/javascript">
        (function () {
            var raw = '{{PosJson}}';
            if (!raw || raw.trim() === '') return;
        
            var data;
            try {
                data = JSON.parse(raw);
            } catch (e) {
                return;
            }
        
            if (!data.pos) return;
        
            var html = '';
            for (var pos in data.pos) {
                html += '<h6>' + pos + '</h6>';
        
                var list = data.pos[pos];
                for (var i = 0; i < list.length; i++) {
                    var d = list[i];
                    html += '<p>' + (d.definition || '-') + '<br>';
                    html += '<i>' + (d.example || '') + '</i></p>';
                }
            }
        
            document.getElementById('pos').innerHTML = html;
        })();
        </script>
        """
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

}
