package com.enesy.bookmarker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.screens.ApiKeyScreen
import com.enesy.bookmarker.ui.screens.WelcomeScreen
import com.enesy.bookmarker.ui.viewmodels.SettingsViewModel

@Composable
fun OnboardingNavGraph(strings: BookmarkerStrings) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                strings = strings,
                onNavigateToNext = {
                navController.navigate("apikey")
            })
        }
        composable("apikey") {
            ApiKeyScreen(
                strings = strings,
                viewModel = settingsViewModel,
                onNavigateToHome = {
                    settingsViewModel.finishOnboarding()
                }
            )
        }
    }
}
