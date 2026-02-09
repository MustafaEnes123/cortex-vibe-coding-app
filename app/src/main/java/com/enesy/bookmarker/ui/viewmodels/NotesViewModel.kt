package com.enesy.bookmarker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.enesy.bookmarker.BookmarkerApp
import com.enesy.bookmarker.data.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(
    private val dao: BookmarkerDao,
    private val syncRepo: SyncRepository,
    private val themeRepo: ThemeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val notes: StateFlow<List<Note>> = combine(
        _searchQuery,
        dao.getAllNotes()
    ) { query, list ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.content.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addNote(content: String) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            val note = Note(
                bookmarkId = 0, // 0 for a standalone note
                content = content,
                createdAt = System.currentTimeMillis(),
                folderId = null, // No folder for notes
                tags = ""
            )
            val id = dao.insertNote(note)
            if (uid != null && isAutoSync) {
                val syncedNote = note.copy(id = id)
                syncRepo.mirrorNote(uid, syncedNote)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            dao.deleteNote(note)
            if (uid != null && isAutoSync) {
                syncRepo.deleteRemoteNote(uid, note.id)
            }
        }
    }

    fun updateNote(note: Note, newContent: String, tags: String) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            val updatedNote = note.copy(content = newContent, tags = tags)
            dao.updateNote(updatedNote)
            if (uid != null && isAutoSync) {
                syncRepo.mirrorNote(uid, updatedNote)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BookmarkerApp)
                NotesViewModel(app.database.bookmarkerDao(), app.syncRepository, app.themeRepository)
            }
        }
    }
}
