package com.iwsocorp.vocabnotes.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iwsocorp.vocabnotes.core.database.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateUpdatedAt(id: String, updatedAt: Long)

    @Query("UPDATE notes SET contentSize = :contentSize WHERE id = :id")
    suspend fun updateContentSize(id: String, contentSize: Int)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

}