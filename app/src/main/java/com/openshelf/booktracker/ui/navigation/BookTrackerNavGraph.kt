package com.openshelf.booktracker.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openshelf.booktracker.ui.screens.detail.BookDetailScreen
import com.openshelf.booktracker.ui.screens.library.LibraryScreen
import com.openshelf.booktracker.ui.screens.stats.StatsScreen

@Composable
fun BookTrackerNavGraph(
    startOnStats: Boolean = false,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = bottomBarScreens.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon(),
                                    contentDescription = screen.label()
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label(),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startOnStats) Screen.Stats.route else Screen.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    }
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen()
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(
                    navArgument(Screen.BookDetail.ARG_BOOK_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val bookId = backStackEntry.arguments
                    ?.getLong(Screen.BookDetail.ARG_BOOK_ID) ?: -1L
                BookDetailScreen(
                    bookId = bookId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun Screen.label(): String = when (this) {
    Screen.Library -> "Library"
    Screen.Stats -> "Stats"
    else -> ""
}

private fun Screen.icon(): ImageVector = when (this) {
    Screen.Library -> Icons.AutoMirrored.Filled.MenuBook
    Screen.Stats -> Icons.Filled.BarChart
    else -> Icons.Filled.BarChart
}

// Unused but kept for reference if you ever need an inline placeholder
@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}