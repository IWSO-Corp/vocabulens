package com.iwsocorp.vobynotes.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE corpus_new (
                id TEXT NOT NULL PRIMARY KEY,
                noteId TEXT NOT NULL,
                word TEXT NOT NULL,
                meaning TEXT NOT NULL,
                wordLang TEXT NOT NULL,
                meaningLang TEXT NOT NULL,
                phonetic TEXT NOT NULL,
                audio TEXT NOT NULL,
                meanings TEXT NOT NULL,
                hasMeaning INTEGER NOT NULL DEFAULT 0,
                mark TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER,
                ankiNoteId INTEGER,
                exportedToAnkiAt INTEGER,
                FOREIGN KEY(noteId) REFERENCES note(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            INSERT INTO corpus_new (
                id, noteId, word, meaning,
                wordLang, meaningLang,
                phonetic, audio, meanings,
                hasMeaning, mark,
                createdAt, updatedAt,
                deletedAt, ankiNoteId, exportedToAnkiAt
            )
            SELECT
                id,
                noteId,
                word,
                meaning,
                wordLang,
                meaningLang,
                IFNULL(phonetic, ''),
                IFNULL(audio, ''),
                IFNULL(meanings, '[]'),
                IFNULL(hasMeaning, 0),
                IFNULL(mark, ''),
                createdAt,
                IFNULL(updatedAt, createdAt),
                deletedAt,
                ankiNoteId,
                exportedToAnkiAt
            FROM corpus
        """)

        db.execSQL("DROP TABLE corpus")
        db.execSQL("ALTER TABLE corpus_new RENAME TO corpus")

        // Recreate indexes
        db.execSQL("CREATE INDEX index_corpus_noteId ON corpus(noteId)")
        db.execSQL("CREATE UNIQUE INDEX index_corpus_word_lang ON corpus(word, wordLang, meaningLang)")
        db.execSQL("CREATE INDEX index_corpus_exportedToAnkiAt ON corpus(exportedToAnkiAt)")
        db.execSQL("CREATE INDEX index_corpus_ankiNoteId ON corpus(ankiNoteId)")
        db.execSQL("CREATE INDEX index_corpus_note_exported ON corpus(noteId, exportedToAnkiAt)")
        db.execSQL("CREATE INDEX index_corpus_note_exported_hasMeaning ON corpus(noteId, exportedToAnkiAt, hasMeaning)")
        db.execSQL("CREATE INDEX index_corpus_note_exported_anki ON corpus(noteId, exportedToAnkiAt, ankiNoteId)")
    }
}