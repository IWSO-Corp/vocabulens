package com.iwsocorp.vocabnotes.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.iwsocorp.vocabnotes.core.database.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): PagingSource<Int, NoteEntity>

    @Transaction
    suspend fun incrementContentSizeAndUpdate(noteId: String) {
        incrementContentSize(noteId)
        updateTimestamp(noteId, System.currentTimeMillis())
    }

    @Query("UPDATE notes SET contentSize = contentSize + 1 WHERE id = :noteId")
    suspend fun incrementContentSize(noteId: String)

    @Query("UPDATE notes SET updatedAt = :time WHERE id = :noteId")
    suspend fun updateTimestamp(noteId: String, time: Long)
}