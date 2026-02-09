package com.enesy.bookmarker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AllInbox
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.enesy.bookmarker.R
import com.enesy.bookmarker.data.Bookmark
import com.enesy.bookmarker.data.Folder
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.ProfileAvatar
import com.enesy.bookmarker.ui.UserProfileDialog
import com.enesy.bookmarker.ui.theme.BookmarkerTheme
import com.enesy.bookmarker.ui.viewmodels.HomeViewModel
import com.enesy.bookmarker.ui.viewmodels.SettingsViewModel
import com.enesy.bookmarker.util.unsplashImages
import java.net.URI

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    strings: BookmarkerStrings,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {

    val bookmarks by viewModel.bookmarks.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val summary by viewModel.summaryState
    val isSummarizing by viewModel.isSummarizing
    val searchQuery by viewModel.searchQuery.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val isAiEnabled = !apiKey.isNullOrBlank()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var managingFolder by remember { mutableStateOf<Folder?>(null) }

    val currentUser by settingsViewModel.currentUser.collectAsState()
    val isSyncing by settingsViewModel.isSyncing.collectAsState()
    val autoSyncState by settingsViewModel.autoSyncState.collectAsState()

    if (showProfileDialog && currentUser != null) {
        UserProfileDialog(
            currentUser = currentUser!!,
            isSyncing = isSyncing,
            autoSyncState = autoSyncState,
            onDismiss = { showProfileDialog = false },
            onSignOut = { settingsViewModel.signOut(); showProfileDialog = false },
            onTriggerManualSync = { settingsViewModel.triggerManualSync() },
            onSetAutoSync = { settingsViewModel.setAutoSync(it) }
        )
    }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = { TextField(value = newFolderName, onValueChange = { newFolderName = it }, placeholder = { Text("Folder name") }) },
            confirmButton = { Button(onClick = { viewModel.createFolder(newFolderName); showNewFolderDialog = false; newFolderName = "" }) { Text("Create") } },
            dismissButton = { Button(onClick = { showNewFolderDialog = false }) { Text("Cancel") } }
        )
    }

    editingBookmark?.let {
        EditBookmarkDialog(
            bookmark = it,
            folders = folders,
            onDismiss = { editingBookmark = null },
            onSave = { bookmark, title, folderId, tags ->
                viewModel.updateBookmark(bookmark.copy(title = title), folderId, tags)
                editingBookmark = null
            }
        )
    }

    managingFolder?.let { folder ->
        FolderManagementDialog(
            folder = folder,
            allBookmarks = viewModel.allBookmarks.collectAsState().value,
            onDismiss = { managingFolder = null },
            onDeleteFolder = {
                viewModel.deleteFolder(folder)
                managingFolder = null
            },
            onUpdateBookmarkFolder = { bm, newId ->
                viewModel.updateBookmark(bm.copy(folderId = newId), newId, bm.tags)
            },
            strings = settingsViewModel.strings.collectAsState().value
        )
    }

    summary?.let {
        AlertDialog(
            onDismissRequest = { viewModel.clearSummaryState() },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Summary") },
            text = { Text(it) },
            confirmButton = { Button(onClick = { viewModel.saveSummaryToNotes() }) { Text("Save to Notes") } },
            dismissButton = { Button(onClick = { viewModel.clearSummaryState() }) { Text("Ignore") } }
        )
    }

    if (isSummarizing) {
        AlertDialog(onDismissRequest = {}, title = { Text("Summarizing...") }, text = { CircularProgressIndicator() }, confirmButton = {})
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            // Search Bar & Folders (Fixed Top Matte Header)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(1f)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)),
                color = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                        .padding(bottom = 16.dp)
                ) {
                    // Search Bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 10.dp, bottom = 16.dp), // Reduced top padding
                        shape = CircleShape,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                            Spacer(modifier = Modifier.width(8.dp))
                            TextField(
                                value = searchQuery, onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(strings.searchHint) },
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            currentUser?.let {
                                ProfileAvatar(photoUrl = it.photoUrl) { showProfileDialog = true }
                            }
                        }
                    }

                    // Folder Collection
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        item {
                            FolderCollectionCard(
                                folderName = strings.allFolder,
                                isSelected = selectedFolder == null,
                                iconSource = Icons.Outlined.AllInbox,
                                onClick = { viewModel.onFolderSelected(null) }
                            )
                        }
                        items(items = folders, key = { it.id }) { folder ->
                            FolderCollectionCard(
                                folderName = folder.name,
                                isSelected = selectedFolder?.id == folder.id,
                                iconSource = Icons.Outlined.Folder,
                                onClick = { viewModel.onFolderSelected(folder) },
                                onLongClick = {
                                    if (folder.id != 0L) managingFolder = folder
                                }
                            )
                        }
                        item {
                            NewFolderCard(onClick = { showNewFolderDialog = true })
                        }
                    }
                }
            }

            // Scrollable Content
            if (bookmarks.isEmpty()) {
                EmptyState(strings)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 220.dp, // Increased from 180.dp to 220.dp to avoid overlap with larger header
                        bottom = 160.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = bookmarks, key = { it.id }) { bookmark ->
                        BookmarkCard(
                            bookmark = bookmark,
                            isAiEnabled = isAiEnabled,
                            onSummarizeClick = {
                                if (isAiEnabled) {
                                    viewModel.summarizeBookmark(bookmark)
                                } else {
                                    Toast.makeText(context, "Please add API Key in Settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteClick = { viewModel.deleteBookmark(bookmark) },
                            onEditClick = { editingBookmark = bookmark }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderManagementDialog(
    folder: Folder,
    allBookmarks: List<Bookmark>,
    onDismiss: () -> Unit,
    onDeleteFolder: () -> Unit,
    onUpdateBookmarkFolder: (Bookmark, Long) -> Unit, // New folder ID
    strings: BookmarkerStrings // For translation
) {
    // Separate bookmarks into Included vs Available
    val included = allBookmarks.filter { it.folderId == folder.id }
    val available = allBookmarks.filter { it.folderId != folder.id }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Included, 1=Available

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                // Delete Button (Small, Error Color)
                TextButton(onClick = onDeleteFolder) {
                    Text(strings.folderDeleteBtn, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column {
                // Custom Tab Row (Segmented look)
                Row(Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50))
                            .clickable { selectedTab = 0 }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.folderTabIncluded, style = MaterialTheme.typography.labelLarge)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selectedTab == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50))
                            .clickable { selectedTab = 1 }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(strings.folderTabAdd, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // List
                LazyColumn(Modifier.height(300.dp)) {
                    val itemsToShow = if (selectedTab == 0) included else available
                    items(itemsToShow) { bookmark ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newId = if (selectedTab == 0) 0L else folder.id
                                    onUpdateBookmarkFolder(bookmark, newId)
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(bookmark.title, maxLines = 1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            // Icon: Minus if included, Plus if available
                            val icon = if (selectedTab == 0) Icons.Default.Remove else Icons.Default.Add
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCollectionCard(
    folderName: String,
    isSelected: Boolean,
    iconSource: Any,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    Card(
        modifier = Modifier
            .size(90.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.TopStart)) {
                when (iconSource) {
                    is ImageVector -> Icon(imageVector = iconSource, contentDescription = null, tint = contentColor)
                    is Int -> Icon(painter = painterResource(id = iconSource), contentDescription = null, tint = contentColor)
                }
            }
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
fun NewFolderCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_action_add),
                contentDescription = "Add New Folder",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun BookmarkCard(
    bookmark: Bookmark,
    isAiEnabled: Boolean,
    onSummarizeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val imageUrl = bookmark.thumbnail ?: unsplashImages[(bookmark.id % unsplashImages.size).toInt()]
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) { // Or a fixed height like 120.dp
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Bookmark Header",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, cardBackgroundColor),
                                startX = 200f // Adjust this value to control the fade
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = URI(bookmark.url).host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSummarizeClick) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Summarize",
                            tint = if (isAiEnabled) MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = onEditClick) { Icon(painter = painterResource(id = R.drawable.ic_action_edit), contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = onDeleteClick) { Icon(painter = painterResource(id = R.drawable.ic_action_delete), contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface) }
                }
            }
        }
    }
}

@Composable
fun EditBookmarkDialog(bookmark: Bookmark, folders: List<Folder>, onDismiss: () -> Unit, onSave: (Bookmark, String, Long?, String) -> Unit) {
    var title by remember { mutableStateOf(bookmark.title) }
    var selectedFolderId by remember { mutableStateOf(bookmark.folderId) }
    var tags by remember { mutableStateOf(bookmark.tags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Bookmark") },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Folder", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(folders) { folder ->
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { selectedFolderId = folder.id },
                            label = { Text(folder.name) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") },
                    shape = CircleShape
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(bookmark, title, selectedFolderId, tags) }) { Text("Save") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyState(strings: BookmarkerStrings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ill_empty_bookmarks),
            contentDescription = strings.noBookmarks,
            modifier = Modifier
                .size(220.dp)
                .padding(bottom = 24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
        Text(
            text = strings.noBookmarks,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BookmarkerTheme {
        // HomeScreen(EnStrings)
    }
}
