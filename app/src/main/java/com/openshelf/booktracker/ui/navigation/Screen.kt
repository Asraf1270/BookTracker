package com.openshelf.booktracker.ui.navigation

sealed class Screen(val route: String) {

    data object Library : Screen("library")

    data object Stats : Screen("stats")

    data object BookDetail : Screen("book/{bookId}") {
        const val ARG_BOOK_ID = "bookId"
        fun createRoute(bookId: Long): String = "book/$bookId"
    }
}

/**
 * Bottom-nav destinations only. Used by the NavBar in Phase 4.
 */
val bottomBarScreens = listOf(Screen.Library, Screen.Stats)