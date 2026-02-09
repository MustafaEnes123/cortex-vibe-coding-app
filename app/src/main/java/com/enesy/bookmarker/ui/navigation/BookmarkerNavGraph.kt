package com.enesy.bookmarker.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enesy.bookmarker.R
import com.enesy.bookmarker.domain.BookmarkerStrings
import com.enesy.bookmarker.ui.screens.ApiKeyScreen
import com.enesy.bookmarker.ui.screens.AssistantScreen
import com.enesy.bookmarker.ui.screens.HomeScreen
import com.enesy.bookmarker.ui.screens.NotesScreen
import com.enesy.bookmarker.ui.screens.SettingsScreen
import com.enesy.bookmarker.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

sealed class Screen(
    val route: String,
    val title: String,
    @DrawableRes val icon: Int
) {
    object Home : Screen("home", "Home", R.drawable.ic_nav_home)
    object Notes : Screen("notes", "Notes", R.drawable.ic_nav_notes)
    object Assistant : Screen("assistant", "Assistant", R.drawable.ic_nav_ai)
    object Settings : Screen("settings", "Settings", R.drawable.ic_nav_settings)
    object ApiKey : Screen("apikey", "API Key", 0) // Icon not needed for this screen
}

@Composable
fun BookmarkerNavGraph(strings: BookmarkerStrings) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController = navController, strings = strings)
        }
        composable(Screen.ApiKey.route) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
            ApiKeyScreen(
                strings = strings,
                viewModel = settingsViewModel,
                onNavigateToHome = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(navController: NavController, strings: BookmarkerStrings) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val selectedIndex by remember { derivedStateOf { pagerState.currentPage } }

    val items = listOf(
        Screen.Home,
        Screen.Notes,
        Screen.Assistant,
        Screen.Settings
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                Box(
                    modifier = Modifier.graphicsLayer {
                        // Calculate offset relative to this page
                        val pageOffset = (
                            (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        ).absoluteValue

                        // iOS Style Logic:
                        // 1. Slight scale down for off-screen pages (0.85 -> 1.0)
                        val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale

                        // 2. Adjust Alpha (Fade out as it leaves)
                        alpha = lerp(0.5f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                    }
                ) {
                    when (page) {
                        0 -> HomeScreen(strings)
                        1 -> NotesScreen(strings)
                        2 -> AssistantScreen(strings, onNavigateToNotes = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        })
                        3 -> SettingsScreen(strings, navController = navController)
                    }
                }
            }
        }

        val isRootDestination = selectedIndex < 2 || selectedIndex == 3

        AnimatedVisibility(
            visible = isRootDestination,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.8f) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.8f) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingBottomBar(
                selectedIndex = selectedIndex,
                pagerState = pagerState,
                scope = coroutineScope,
                items = items
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingBottomBar(selectedIndex: Int, pagerState: PagerState, scope: CoroutineScope, items: List<Screen>) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    NavigationBar(
        modifier = Modifier
            .padding(start = 48.dp, end = 48.dp, bottom = 16.dp)
            .navigationBarsPadding()
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .border(BorderStroke(0.5.dp, borderColor), CircleShape),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEachIndexed { index, screen ->
            val isSelected = selectedIndex == index
            val scale by animateFloatAsState(if (isSelected) 1.2f else 1.0f)

            NavigationBarItem(
                icon = {
                    val iconSize = if (index == 3) 22.dp else 24.dp
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.title,
                        modifier = Modifier
                            .scale(scale)
                            .size(iconSize),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = null,
                selected = isSelected,
                onClick = {
                    if (selectedIndex != index) {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                },
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
