package com.enesy.bookmarker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.enesy.bookmarker.ui.navigation.BookmarkerNavGraph
import com.enesy.bookmarker.ui.navigation.OnboardingNavGraph
import com.enesy.bookmarker.ui.theme.BookmarkerTheme
import com.enesy.bookmarker.ui.theme.CortexBackground
import com.enesy.bookmarker.ui.viewmodels.SettingsViewModel
import com.enesy.bookmarker.domain.BookmarkerStrings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        val settingsViewModel: SettingsViewModel by viewModels { SettingsViewModel.Factory }

        // Keep splash visible while state is NULL (Loading)
        splashScreen.setKeepOnScreenCondition {
            settingsViewModel.onboardingState.value == null
        }

        setContent {
            val systemTheme = isSystemInDarkTheme()
            val savedThemeState by settingsViewModel.themeState.collectAsState()
            val isDarkTheme = savedThemeState ?: systemTheme
            val onboardingState by settingsViewModel.onboardingState.collectAsState()
            val strings by settingsViewModel.strings.collectAsState()

            BookmarkerTheme(darkTheme = isDarkTheme) {
                CortexBackground(isDark = isDarkTheme) {
                    // Once state is NOT null, showing content happens immediately
                    when (onboardingState) {
                        true -> MainAppContent(strings)
                        false -> OnboardingNavGraph(strings)
                        else -> { /* Splash screen handles the loading state */ }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(strings: BookmarkerStrings) {
    BookmarkerNavGraph(strings)
}
