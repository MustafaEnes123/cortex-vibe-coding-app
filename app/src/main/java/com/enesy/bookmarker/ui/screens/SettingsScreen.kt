package com.enesy.bookmarker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.theme.BookmarkerTheme
import com.enesy.bookmarker.ui.viewmodels.SettingsViewModel
import com.enesy.bookmarker.ui.viewmodels.WelcomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    strings: BookmarkerStrings,
    settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
    welcomeViewModel: WelcomeViewModel = viewModel(),
    navController: NavController
) {
    val themeState by settingsViewModel.themeState.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkMode = themeState ?: isSystemDark
    val currentUser by settingsViewModel.currentUser.collectAsState()
    val apiKey by settingsViewModel.apiKey.collectAsState()
    val currentLang by settingsViewModel.currentLanguage.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    // TODO: Export data to uri
                }
            }
        }
    )

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    // TODO: Import data from uri
                }
            }
        }
    )

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Account Card
            SettingsGroup(strings.account) {
                if (currentUser != null) {
                    Row(verticalAlignment = Alignment.Top) {
                        AsyncImage(
                            model = currentUser?.photoUrl,
                            contentDescription = "User profile picture",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(currentUser?.displayName ?: "", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(currentUser?.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { settingsViewModel.signOut() },
                                modifier = Modifier.heightIn(min = 32.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text(strings.logout, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign in to sync your Second Brain.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { coroutineScope.launch { welcomeViewModel.signInWithGoogle(context) } }) {
                            Text(strings.googleSignBtn)
                        }
                    }
                }
            }

            // Language Configuration
            SettingsGroup(strings.language) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 2) {
                    LanguageButton(text = "🇺🇸 English", isSelected = currentLang == "en", onClick = { settingsViewModel.setLanguage("en") }, modifier = Modifier.weight(1f))
                    LanguageButton(text = "🇹🇷 Türkçe", isSelected = currentLang == "tr", onClick = { settingsViewModel.setLanguage("tr") }, modifier = Modifier.weight(1f))
                    LanguageButton(text = "🇩🇪 Deutsch", isSelected = currentLang == "de", onClick = { settingsViewModel.setLanguage("de") }, modifier = Modifier.weight(1f))
                    LanguageButton(text = "🇪🇸 Español", isSelected = currentLang == "es", onClick = { settingsViewModel.setLanguage("es") }, modifier = Modifier.weight(1f))
                    LanguageButton(text = "🇫🇷 Français", isSelected = currentLang == "fr", onClick = { settingsViewModel.setLanguage("fr") }, modifier = Modifier.weight(1f))
                    LanguageButton(text = "🇷🇺 Русский", isSelected = currentLang == "ru", onClick = { settingsViewModel.setLanguage("ru") }, modifier = Modifier.weight(1f))
                }
            }

            // AI Configuration
            SettingsGroup(strings.aiConnection) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("API Key", fontWeight = FontWeight.Bold)
                        Text("Status: ${if (apiKey.isNullOrEmpty()) "Not Connected" else "Active"}", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { navController.navigate("apikey") }) {
                        Text(if (apiKey.isNullOrEmpty()) "Add Key" else "Update Key")
                    }
                }
            }

            // Theme Group
            SettingsGroup(strings.darkMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.darkMode, style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { settingsViewModel.toggleTheme(it) }
                    )
                }
            }

            // Data Group
            SettingsGroup(strings.backup) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    BackupButton(
                        text = strings.restore,
                        icon = Icons.Outlined.ArrowUpward,
                        onClick = { exportLauncher.launch("bookmarker_backup.json") },
                        modifier = Modifier.weight(1f)
                    )
                    BackupButton(
                        text = strings.backup,
                        icon = Icons.Outlined.ArrowDownward,
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Version Info
            Text(
                text = "Cortex 1.2.6",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(120.dp))

        }
    }
}

@Composable
fun LanguageButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(text)
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun BackupButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = text)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BookmarkerTheme {
        // SettingsScreen(EnStrings, navController = rememberNavController())
    }
}
