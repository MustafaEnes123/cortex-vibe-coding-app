package com.enesy.bookmarker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkerDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(items: List<Bookmark>)

    @Query("SELECT * FROM bookmarks ORDER BY id DESC")
    fun getAllBookmarksFlow(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksList(): List<Bookmark>

    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY id DESC")
    fun getBookmarksByFolderFlow(folderId: Long): Flow<List<Bookmark>>

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(items: List<Note>)

    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesList(): List<Note>

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: Folder): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<Folder>)

    @Query("SELECT * FROM folders ORDER BY id ASC")
    fun getAllFoldersFlow(): Flow<List<Folder>>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersList(): List<Folder>

    @Query("SELECT * FROM folders")
    suspend fun getFolders(): List<Folder>

    @Delete
    suspend fun deleteFolder(folder: Folder)
}
