package com.openshelf.booktracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme

@Composable
fun BookTrackerNavGraph(
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
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Library.route) {
                // Phase 4 will replace with LibraryScreen(...)
                PlaceholderScreen("Library")
            }

            composable(Screen.Stats.route) {
                // Phase 6 will replace with StatsScreen(...)
                PlaceholderScreen("Stats")
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(
                    navArgument(Screen.BookDetail.ARG_BOOK_ID) { type = NavType.LongType }
                )
            ) {
                // Phase 5 will replace with BookDetailScreen(...)
                PlaceholderScreen("Book Detail")
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

@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}