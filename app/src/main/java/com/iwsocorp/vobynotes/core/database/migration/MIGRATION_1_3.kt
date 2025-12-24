package com.iwsocorp.vobynotes.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // Tambah kolom satu per satu (AMAN)
        if (!db.hasColumn("corpus", "ankiNoteId")) {
            db.execSQL("ALTER TABLE corpus ADD COLUMN ankiNoteId INTEGER")
        }

        if (!db.hasColumn("corpus", "exportedToAnkiAt")) {
            db.execSQL("ALTER TABLE corpus ADD COLUMN exportedToAnkiAt INTEGER")
        }

        if (!db.hasColumn("corpus", "hasMeaning")) {
            db.execSQL(
                "ALTER TABLE corpus ADD COLUMN hasMeaning INTEGER NOT NULL DEFAULT 0"
            )
        }

        // Re-sync data
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

fun SupportSQLiteDatabase.hasColumn(
    table: String,
    column: String
): Boolean {
    query("PRAGMA table_info($table)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}