package com.iwsocorp.vobynotes.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.iwsocorp.vobynotes.core.database.model.NoteEntity

@Dao
interface NoteDao {

    @Insert
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteList(noteList: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): NoteEntity

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): PagingSource<Int, NoteEntity>

    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<NoteEntity>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getNoteList(): List<NoteEntity>

    @Query("UPDATE notes SET contentSize = contentSize + :count, updatedAt = :time WHERE id = :noteId")
    suspend fun incrementContentSize(noteId: String, count: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET contentSize = contentSize - :count WHERE id = :noteId")
    suspend fun decrementContentSize(noteId: String, count: Int)

    @Query("UPDATE notes SET updatedAt = :time WHERE id = :noteId")
    suspend fun updateTimestamp(noteId: String, time: Long)
}