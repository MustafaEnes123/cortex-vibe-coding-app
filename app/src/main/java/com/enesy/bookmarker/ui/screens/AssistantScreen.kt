package com.enesy.bookmarker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enesy.bookmarker.data.ItemType
import com.enesy.bookmarker.data.SelectableItem
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.features.AiFeature
import com.enesy.bookmarker.ui.viewmodels.AssistantViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(strings: BookmarkerStrings, onNavigateToNotes: () -> Unit) {
    val viewModel: AssistantViewModel = viewModel(factory = AssistantViewModel.Factory)
    val uiState = viewModel.uiState
    val apiKey by viewModel.apiKey.collectAsState()
    val isAiEnabled = !apiKey.isNullOrBlank()
    val sheetState = rememberModalBottomSheetState()
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // HEADER ROW
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FloatingBackButton(onClick = onNavigateToNotes)
                Text(
                    text = strings.aiHubTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            // FEATURE GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(AiFeature.values()) { feature ->
                    AiFeatureTile(feature = feature, isEnabled = isAiEnabled, strings = strings) {
                        if (isAiEnabled) {
                            viewModel.onFeatureClicked(feature)
                        } else {
                            Toast.makeText(context, "Enter API Key to unlock", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    if (uiState.showContentSelectionDialog) {
        ContentSelectionDialog(
            items = uiState.selectableItems,
            onDismiss = viewModel::onDialogDismiss,
            onConfirm = viewModel::runAnalysis,
            onItemClick = viewModel::toggleItemSelection
        )
    }

    if (uiState.isLoading || uiState.analysisResult != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!uiState.isLoading) {
                    viewModel.clearResult()
                }
            },
            sheetState = sheetState
        ) {
            ResultSheetContent(
                isLoading = uiState.isLoading,
                result = uiState.analysisResult,
                onSaveAsNote = {
                     viewModel.saveResultAsNote()
                }
            )
        }
    }
}

@Composable
fun AiFeatureTile(
    feature: AiFeature, 
    isEnabled: Boolean, 
    strings: BookmarkerStrings,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = if (isEnabled) Modifier else Modifier.alpha(0.5f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Icon(
                painter = painterResource(id = feature.iconRes),
                contentDescription = feature.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when(feature) {
                    AiFeature.QUICK_RECAP -> strings.featureRecap
                    AiFeature.ACTION_PLAN -> strings.featurePlan
                    AiFeature.ELI5 -> strings.featureEli5
                    AiFeature.IDEA_GENERATOR -> strings.featureIdea
                    AiFeature.CONNECT_DOTS -> strings.featureConnect
                    AiFeature.QUIZ_ME -> strings.featureQuiz
                    AiFeature.DEBATE_MASTER -> strings.featureDebate
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ContentSelectionDialog(
    items: List<SelectableItem>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onItemClick: (SelectableItem) -> Unit
) {
    var filter by remember { mutableStateOf<ItemType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Content") },
        text = {
            Column {
                // Filter Tabs
                TabRow(selectedTabIndex = when(filter) { null -> 0; ItemType.BOOKMARK -> 1; ItemType.NOTE -> 2 }) {
                    Tab(selected = filter == null, onClick = { filter = null }, text = { Text("All") })
                    Tab(selected = filter == ItemType.BOOKMARK, onClick = { filter = ItemType.BOOKMARK }, text = { Text("Bookmarks") })
                    Tab(selected = filter == ItemType.NOTE, onClick = { filter = ItemType.NOTE }, text = { Text("Notes") })
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                    val filteredList = items.filter { filter == null || it.type == filter }
                    items(filteredList) { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isSelected,
                                onCheckedChange = { onItemClick(item) }
                            )
                            Text(item.title, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Run Analysis")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ResultSheetContent(isLoading: Boolean, result: String?, onSaveAsNote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(400.dp) 
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (result != null) {
            Column(modifier = Modifier.weight(1f)) {
                 Text("Analysis Result", style = MaterialTheme.typography.titleLarge)
                 Spacer(modifier = Modifier.height(16.dp))
                 Box(
                     modifier = Modifier
                         .verticalScroll(rememberScrollState())
                         .weight(1f)
                 ) {
                     Text(result)
                 }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSaveAsNote, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Save as Note")
            }
        }
    }
}


// Keep/Refine the FloatingBackButton helper function
@Composable
fun FloatingBackButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
    }
}
