package com.enesy.bookmarker.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.enesy.bookmarker.BookmarkerApp
import com.enesy.bookmarker.data.AuthRepository
import com.enesy.bookmarker.data.BookmarkerDao
import com.enesy.bookmarker.data.SyncRepository
import com.enesy.bookmarker.data.ThemeRepository
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.domain.DeStrings
import com.enesy.bookmarker.domain.EnStrings
import com.enesy.bookmarker.domain.EsStrings
import com.enesy.bookmarker.domain.FrStrings
import com.enesy.bookmarker.domain.RuStrings
import com.enesy.bookmarker.domain.TrStrings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val dao: BookmarkerDao
) : ViewModel() {

    private val _currentUser = MutableStateFlow(FirebaseAuth.getInstance().currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val apiKey: StateFlow<String?> = themeRepository.getApiKey()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val themeState: StateFlow<Boolean?> = themeRepository.getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val onboardingState: StateFlow<Boolean?> = themeRepository.onboardingState
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val autoSyncState: StateFlow<Boolean> = themeRepository.autoSyncState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true
        )

    val currentLanguage = themeRepository.getLanguage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "en"
        )

    val strings: StateFlow<BookmarkerStrings> = currentLanguage.map { code ->
        when(code) {
            "tr" -> TrStrings
            "de" -> DeStrings
            "es" -> EsStrings
            "fr" -> FrStrings
            "ru" -> RuStrings
            else -> EnStrings
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EnStrings)

    fun setLanguage(code: String) {
        viewModelScope.launch {
            themeRepository.saveLanguage(code)
        }
    }

    fun toggleTheme(isDark: Boolean) {
        viewModelScope.launch {
            themeRepository.saveThemeMode(isDark)
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            themeRepository.saveApiKey(key)
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            themeRepository.completeOnboarding()
        }
    }

    fun setAutoSync(isAutoSync: Boolean) {
        viewModelScope.launch {
            themeRepository.saveAutoSyncState(isAutoSync)
        }
    }

    fun triggerManualSync() {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        _isSyncing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Pull (Restore) first
                syncRepository.pullFromCloud(uid)

                // 2. Push (Backup) local data to Cloud
                val allBookmarks = dao.getAllBookmarksList()
                allBookmarks.forEach { syncRepository.mirrorBookmark(uid, it) }

                val allNotes = dao.getAllNotesList()
                allNotes.forEach { syncRepository.mirrorNote(uid, it) }

                val allFolders = dao.getAllFoldersList()
                allFolders.forEach { syncRepository.mirrorFolder(uid, it) }

            } catch (e: Exception) {
                e.printStackTrace() // Don't crash
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _currentUser.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BookmarkerApp)
                SettingsViewModel(
                    themeRepository = app.themeRepository,
                    authRepository = app.authRepository,
                    syncRepository = app.syncRepository,
                    dao = app.database.bookmarkerDao()
                )
            }
        }
    }
}
