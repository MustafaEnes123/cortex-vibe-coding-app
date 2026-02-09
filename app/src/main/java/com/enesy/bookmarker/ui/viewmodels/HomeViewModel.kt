package com.enesy.bookmarker.ui.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.enesy.bookmarker.BookmarkerApp
import com.enesy.bookmarker.data.Bookmark
import com.enesy.bookmarker.data.BookmarkerDao
import com.enesy.bookmarker.data.Folder
import com.enesy.bookmarker.data.Note
import com.enesy.bookmarker.data.SyncRepository
import com.enesy.bookmarker.data.ThemeRepository
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.domain.EnStrings
import com.enesy.bookmarker.domain.TrStrings
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val dao: BookmarkerDao,
    private val syncRepo: SyncRepository,
    private val themeRepo: ThemeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    private var generativeModel: GenerativeModel? = null

    val apiKey: StateFlow<String?> = themeRepo.getApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val currentLanguage = themeRepo.getLanguage()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    val strings: StateFlow<BookmarkerStrings> = currentLanguage.map { code ->
        when (code) {
            "tr" -> TrStrings
            else -> EnStrings
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EnStrings)

    init {
        viewModelScope.launch {
            apiKey.collect {
                if (!it.isNullOrBlank()) {
                    generativeModel = GenerativeModel(
                        modelName = "gemini-3-flash-preview",
                        apiKey = it
                    )
                }
            }
        }
    }

    val folders: StateFlow<List<Folder>> = dao.getAllFoldersFlow()
        .map { list -> list.filter { !it.name.equals("All", ignoreCase = true) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<Bookmark>> = dao.getAllBookmarksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<Bookmark>> = combine(
        _selectedFolder,
        _searchQuery,
        dao.getAllBookmarksFlow()
    ) { folder: Folder?, query: String, list: List<Bookmark> ->
        var result = list

        if (folder != null && folder.id != 0L && folder.name != "All") {
            result = result.filter { it.folderId == folder.id }
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summaryState = mutableStateOf<String?>(null)
    val isSummarizing = mutableStateOf(false)
    private var activeBookmark = mutableStateOf<Bookmark?>(null)

    fun onFolderSelected(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            val newFolder = Folder(name = name)
            val id = dao.insertFolder(newFolder)
            if (uid != null && isAutoSync) {
                val syncedFolder = newFolder.copy(id = id)
                syncRepo.mirrorFolder(uid, syncedFolder)
            }
        }
    }

    fun addBookmark(url: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            val newBookmark = Bookmark(
                url = url,
                title = title,
                originalTitle = title,
                platform = "Shared",
                folderId = null,
                isSummarized = false,
                tags = ""
            )
            val id = dao.insertBookmark(newBookmark)
            if (uid != null && isAutoSync) {
                val syncedBookmark = newBookmark.copy(id = id)
                syncRepo.mirrorBookmark(uid, syncedBookmark)
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        if (folder.name.equals("All", ignoreCase = true) || folder.id == 0L) return
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            dao.deleteFolder(folder)
            if (_selectedFolder.value == folder) {
                _selectedFolder.value = null
            }
            if (uid != null && isAutoSync) {
                syncRepo.deleteRemoteFolder(uid, folder.id)
            }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            dao.deleteBookmark(bookmark)
            if (uid != null && isAutoSync) {
                syncRepo.deleteRemoteBookmark(uid, bookmark.id)
            }
        }
    }

    fun updateBookmark(bookmark: Bookmark, folderId: Long?, tags: String) {
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            val updatedBookmark = bookmark.copy(folderId = folderId, tags = tags)
            dao.updateBookmark(updatedBookmark)
            if (uid != null && isAutoSync) {
                syncRepo.mirrorBookmark(uid, updatedBookmark)
            }
        }
    }

    fun summarizeBookmark(bookmark: Bookmark) {
        if (generativeModel == null) {
            summaryState.value = "Error: API Key not set."
            return
        }

        viewModelScope.launch {
            isSummarizing.value = true
            activeBookmark.value = bookmark
            try {
                val currentPrompts = strings.value
                val prompt = "${currentPrompts.promptSummarize}\n\nContext Title: ${bookmark.title}\n\nContext Body: ${bookmark.rawContent}"
                val response = generativeModel!!.generateContent(prompt)
                summaryState.value = response.text
            } catch (e: Exception) {
                summaryState.value = "Error: Could not generate summary. ${e.message}"
            } finally {
                isSummarizing.value = false
            }
        }
    }

    fun saveSummaryToNotes() {
        val summaryText = summaryState.value ?: return
        viewModelScope.launch {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val isAutoSync = themeRepo.autoSyncState.first()
            val note = Note(
                bookmarkId = activeBookmark.value?.id ?: 0L,
                content = summaryText,
                createdAt = System.currentTimeMillis(),
                folderId = _selectedFolder.value?.id ?: 0L,
                tags = "Summary"
            )
            val id = dao.insertNote(note)
            if (uid != null && isAutoSync) {
                val syncedNote = note.copy(id = id)
                syncRepo.mirrorNote(uid, syncedNote)
            }
            summaryState.value = null
        }
    }

    fun clearSummaryState() {
        summaryState.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BookmarkerApp)
                HomeViewModel(app.database.bookmarkerDao(), app.syncRepository, app.themeRepository)
            }
        }
    }
}
