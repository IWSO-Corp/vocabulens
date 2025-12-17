package com.iwsocorp.vobynotes.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // 1. Tambah kolom
        db.execSQL(
            """
            ALTER TABLE corpus
            ADD COLUMN ankiNoteId INTEGER
            ADD COLUMN exportedToAnkiAt INTEGER
            ADD COLUMN hasMeaning INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
        )

        // 2. Isi nilai awal berdasarkan data lama
        // Aman: JSON kosong → tetap 0
        db.execSQL(
            """
            UPDATE corpus
            SET hasMeaning = 
                CASE
                    WHEN meanings IS NOT NULL
                     AND meanings != '[]'
                    THEN 1
                    ELSE 0
                END
            """.trimIndent()
        )
    }
}