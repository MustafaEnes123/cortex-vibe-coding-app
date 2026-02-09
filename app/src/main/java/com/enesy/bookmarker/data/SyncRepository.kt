package com.enesy.bookmarker.data

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class SyncRepository(private val dao: BookmarkerDao) {

    private val firestore = Firebase.firestore

    suspend fun mirrorBookmark(uid: String, bookmark: Bookmark) {
        if (uid.isEmpty()) return
        val data = hashMapOf(
            "id" to bookmark.id,
            "title" to bookmark.title,
            "url" to bookmark.url,
            "folderId" to bookmark.folderId,
            "tags" to bookmark.tags,
            "platform" to bookmark.platform,
            "isSummarized" to bookmark.isSummarized,
            "originalTitle" to bookmark.originalTitle
        )
        firestore.collection("users").document(uid).collection("bookmarks")
            .document(bookmark.id.toString())
            .set(data)
            .await()
    }

    suspend fun mirrorNote(uid: String, note: Note) {
        if (uid.isEmpty()) return
        val data = hashMapOf(
            "id" to note.id,
            "bookmarkId" to note.bookmarkId,
            "content" to note.content,
            "createdAt" to note.createdAt,
            "folderId" to note.folderId,
            "tags" to note.tags
        )
        firestore.collection("users").document(uid).collection("notes")
            .document(note.id.toString())
            .set(data)
            .await()
    }

    suspend fun mirrorFolder(uid: String, folder: Folder) {
        if (uid.isEmpty()) return
        val data = hashMapOf(
            "id" to folder.id,
            "name" to folder.name
        )
        firestore.collection("users").document(uid).collection("folders")
            .document(folder.id.toString())
            .set(data)
            .await()
    }

    suspend fun deleteRemoteBookmark(uid: String, id: Long) {
        if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("bookmarks").document(id.toString()).delete().await()
    }

    suspend fun deleteRemoteNote(uid: String, id: Long) {
        if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("notes").document(id.toString()).delete().await()
    }

    suspend fun deleteRemoteFolder(uid: String, id: Long) {
        if (uid.isEmpty()) return
        firestore.collection("users").document(uid).collection("folders").document(id.toString()).delete().await()
    }

    suspend fun performFullRestore(uid: String) {
        pullFromCloud(uid)
    }

    suspend fun pullFromCloud(uid: String) {
        if (uid.isEmpty()) return

        // Folders
        val folderSnapshot = firestore.collection("users").document(uid).collection("folders").get().await()
        if (!folderSnapshot.isEmpty) {
            val cloudFolders = folderSnapshot.documents.mapNotNull { doc ->
                Folder(
                    id = doc.getLong("id") ?: 0L,
                    name = doc.getString("name") ?: ""
                )
            }
            dao.insertFolders(cloudFolders)
        }

        // Bookmarks
        val bookmarkSnapshot = firestore.collection("users").document(uid).collection("bookmarks").get().await()
        if (!bookmarkSnapshot.isEmpty) {
            val cloudBookmarks = bookmarkSnapshot.documents.mapNotNull { doc ->
                Bookmark(
                    id = doc.getLong("id") ?: 0L,
                    title = doc.getString("title") ?: "",
                    url = doc.getString("url") ?: "",
                    folderId = doc.getLong("folderId"),
                    tags = doc.getString("tags") ?: "",
                    platform = doc.getString("platform") ?: "Web",
                    isSummarized = doc.getBoolean("isSummarized") ?: false,
                    originalTitle = doc.getString("originalTitle") ?: ""
                )
            }
            dao.insertBookmarks(cloudBookmarks)
        }

        // Notes
        val noteSnapshot = firestore.collection("users").document(uid).collection("notes").get().await()
        if (!noteSnapshot.isEmpty) {
            val cloudNotes = noteSnapshot.documents.mapNotNull { doc ->
                Note(
                    id = doc.getLong("id") ?: 0L,
                    bookmarkId = doc.getLong("bookmarkId") ?: 0L,
                    content = doc.getString("content") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    folderId = doc.getLong("folderId"),
                    tags = doc.getString("tags") ?: ""
                )
            }
            dao.insertNotes(cloudNotes)
        }
    }
}
