package com.enesy.bookmarker.ui.viewmodels

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.enesy.bookmarker.BookmarkerApp
import com.enesy.bookmarker.data.*
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.domain.DeStrings
import com.enesy.bookmarker.domain.EnStrings
import com.enesy.bookmarker.domain.EsStrings
import com.enesy.bookmarker.domain.FrStrings
import com.enesy.bookmarker.domain.RuStrings
import com.enesy.bookmarker.domain.TrStrings
import com.enesy.bookmarker.ui.features.AiFeature
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
data class AssistantUiState(
    val showContentSelectionDialog: Boolean = false,
    val selectableItems: List<SelectableItem> = emptyList(),
    val analysisResult: String? = null,
    val isLoading: Boolean = false,
    val currentFeature: AiFeature? = null,
)

class AssistantViewModel(private val dao: BookmarkerDao, private val themeRepo: ThemeRepository) : ViewModel() {

    var uiState by mutableStateOf(AssistantUiState())
        private set

    private var generativeModel: GenerativeModel? = null

    val apiKey: StateFlow<String?> = themeRepo.getApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val currentLanguage = themeRepo.getLanguage()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    val strings: StateFlow<BookmarkerStrings> = currentLanguage.map { code ->
        when (code) {
            "tr" -> TrStrings
            "de" -> DeStrings
            "es" -> EsStrings
            "fr" -> FrStrings
            "ru" -> RuStrings
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

    fun onFeatureClicked(feature: AiFeature) {
        if (apiKey.value.isNullOrBlank()) {
            uiState = uiState.copy(analysisResult = "Error: API Key not set.")
            return
        }
        uiState = uiState.copy(currentFeature = feature, showContentSelectionDialog = true)
        loadSelectableItems()
    }

    fun toggleItemSelection(itemToToggle: SelectableItem) {
        val updatedItems = uiState.selectableItems.map {
            if (it.id == itemToToggle.id && it.type == itemToToggle.type) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
        uiState = uiState.copy(selectableItems = updatedItems)
    }

    private fun loadSelectableItems() {
        viewModelScope.launch {
            val notes = dao.getAllNotes().first().map {
                SelectableItem(it.id, "Note: ${it.content.take(50)}...", it.content, ItemType.NOTE)
            }
            val bookmarks = dao.getAllBookmarksFlow().first().map {
                SelectableItem(it.id, it.title, it.url, ItemType.BOOKMARK)
            }
            uiState = uiState.copy(selectableItems = notes + bookmarks)
        }
    }

    fun onDialogDismiss() {
        val clearedItems = uiState.selectableItems.map { it.copy(isSelected = false) }
        uiState = uiState.copy(showContentSelectionDialog = false, selectableItems = clearedItems)
    }

    fun runAnalysis() {
        val selectedItems = uiState.selectableItems.filter { it.isSelected }
        if (uiState.currentFeature == null || selectedItems.isEmpty()) return

        if (generativeModel == null) {
            uiState = uiState.copy(analysisResult = "Error: API Key not set.")
            return
        }

        uiState = uiState.copy(isLoading = true, showContentSelectionDialog = false)
        val context = selectedItems.joinToString("\n\n") {
            "Data: Item ${it.id} (${it.type}):\n${it.content}"
        }

        val systemInstruction = when(uiState.currentFeature) {
            AiFeature.QUICK_RECAP -> strings.value.promptRecap
            AiFeature.ACTION_PLAN -> strings.value.promptPlan
            AiFeature.ELI5 -> strings.value.promptEli5
            AiFeature.IDEA_GENERATOR -> strings.value.promptIdea
            AiFeature.CONNECT_DOTS -> strings.value.promptConnect
            AiFeature.QUIZ_ME -> strings.value.promptQuiz
            AiFeature.DEBATE_MASTER -> strings.value.promptDebate
            else -> ""
        }

        val prompt = "$systemInstruction\n\n$context"

        viewModelScope.launch {
            try {
                val response = generativeModel!!.generateContent(prompt)
                uiState = uiState.copy(analysisResult = response.text, isLoading = false)
            } catch (e: Exception) {
                uiState = uiState.copy(analysisResult = "Error: ${e.message}", isLoading = false)
            }
        }
    }

    fun clearResult() {
        uiState = uiState.copy(analysisResult = null)
    }

    fun saveResultAsNote() {
        val result = uiState.analysisResult
        val featureTitle = uiState.currentFeature?.title
        if (result == null || featureTitle == null) return

        viewModelScope.launch {
            val noteContent = "## AI Analysis: $featureTitle\n\n$result"
            dao.insertNote(
                Note(
                    content = noteContent,
                    createdAt = System.currentTimeMillis(),
                    bookmarkId = -1, // Not linked to a specific bookmark
                    folderId = null
                )
            )
            clearResult() // Optionally clear the result after saving
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application =
                    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as BookmarkerApp)
                AssistantViewModel(application.database.bookmarkerDao(), application.themeRepository)
            }
        }
    }
}
