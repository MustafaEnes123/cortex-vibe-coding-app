package com.enesy.bookmarker.ui.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.enesy.bookmarker.R
import com.enesy.bookmarker.data.Note
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.ProfileAvatar
import com.enesy.bookmarker.ui.UserProfileDialog
import com.enesy.bookmarker.ui.theme.BookmarkerTheme
import com.enesy.bookmarker.ui.viewmodels.NotesViewModel
import com.enesy.bookmarker.ui.viewmodels.SettingsViewModel
import com.enesy.bookmarker.util.unsplashImages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    strings: BookmarkerStrings,
    viewModel: NotesViewModel = viewModel(factory = NotesViewModel.Factory), 
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var viewingNote by remember { mutableStateOf<Note?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }

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

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onSave = { content ->
                viewModel.addNote(content)
                showAddNoteDialog = false
            }
        )
    }

    editingNote?.let {
        EditNoteDialog(
            note = it,
            onDismiss = { editingNote = null },
            onSave = { note, content, tags ->
                viewModel.updateNote(note, content, tags)
                editingNote = null
            }
        )
    }

    viewingNote?.let { note ->
        AlertDialog(
            onDismissRequest = { viewingNote = null },
            title = { Text(SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(note.createdAt))) },
            text = {
                LazyColumn {
                    item {
                        Text(note.content)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewingNote = null }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { showAddNoteDialog = true },
                modifier = Modifier.padding(bottom = 120.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = strings.addNoteBtn)
            }
        }
    ) { paddingValues ->
        val searchBarHeight = 62.dp // Approximate height of the search bar

        Box(modifier = Modifier.fillMaxSize()) {

            // NOTES LIST / EMPTY STATE - Scrollable Content
            if (notes.isEmpty()) {
                EmptyStateNotes(
                    strings = strings,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = searchBarHeight)
                        .padding(bottom = paddingValues.calculateBottomPadding())
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = searchBarHeight, // Use the defined height to clear the search bar
                        bottom = 160.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { viewingNote = note },
                            onDeleteClick = { viewModel.deleteNote(note) },
                            onEditClick = { editingNote = note }
                        )
                    }
                }
            }

            // TOP SEARCH BAR - Fixed position
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding() // Pushes it below the status bar
                    .offset(y = (-15).dp) // <-- Shift up by 5dp
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp), // Reduced vertical padding to make it thinner
                    shape = CircleShape,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
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
            }
        }
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDeleteClick: () -> Unit, onEditClick: () -> Unit) {
    val imageUrl = unsplashImages[(note.id % unsplashImages.size).toInt()]
    val cardBackgroundColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .crossfade(true)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    contentDescription = "Note Header",
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
                    text = note.content,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(note.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) { Icon(painter = painterResource(id = R.drawable.ic_action_edit), contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface) }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) { Icon(painter = painterResource(id = R.drawable.ic_action_delete), contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface) }
                }
            }
        }
    }
}


@Composable
fun EditNoteDialog(note: Note, onDismiss: () -> Unit, onSave: (Note, String, String) -> Unit) {
    var content by remember { mutableStateOf(note.content) }
    var tags by remember { mutableStateOf(note.tags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Note") },
        shape = RoundedCornerShape(28.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.height(150.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma-separated)") },
                    shape = CircleShape
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(note, content, tags) }) { Text("Save") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Note") },
        text = { OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") }, modifier = Modifier.height(150.dp)) },
        confirmButton = { Button(onClick = { onSave(content) }) { Text("Save") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EmptyStateNotes(strings: BookmarkerStrings, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ill_empty_notes),
            contentDescription = strings.noNotes,
            modifier = Modifier
                .size(220.dp)
                .padding(bottom = 24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
        )
        Text(
            text = strings.noNotes,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotesScreenPreview() {
    BookmarkerTheme {
        // NotesScreen(EnStrings)
    }
}
