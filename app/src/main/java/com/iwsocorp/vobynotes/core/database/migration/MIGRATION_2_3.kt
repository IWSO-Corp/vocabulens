package com.iwsocorp.vobynotes.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {

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

        // Re-sync logic (idempotent)
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
            """
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