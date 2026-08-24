package com.okensun.todayfeed.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.okensun.todayfeed.components.articles.ui.ArticleDetailScreen
import com.okensun.todayfeed.components.articles.ui.SavedScreen
import com.okensun.todayfeed.components.feed.ui.FeedScreen

@Composable
fun TodayFeedApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            // The back stack is read here rather than outside this lambda. A state read
            // recomposes the function that reads it, and only the bar needs these values.
            val backStackEntry by navController.currentBackStackEntryAsState()
            val destination = backStackEntry?.destination
            val onReading = destination?.hasRoute(ReadingRoute::class) == true
            val onSaved = destination?.hasRoute(SavedRoute::class) == true

            // The bar only shows on the two top level destinations, so detail is full screen.
            // It fades over the same time as the destination it is leaving with. Both are told
            // to change at the same moment, so sharing the duration is what keeps them together.
            AnimatedVisibility(
                visible = onReading || onSaved,
                enter = fadeIn(tween(TRANSITION_MILLIS)),
                exit = fadeOut(tween(TRANSITION_MILLIS))
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = onReading,
                        onClick = { navController.switchTab(ReadingRoute) },
                        icon = { Text("Reading") }
                    )
                    NavigationBarItem(
                        selected = onSaved,
                        onClick = { navController.switchTab(SavedRoute) },
                        icon = { Text("Saved") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ReadingRoute,
            modifier = Modifier.padding(padding),
            // Stated rather than left to the library, because the bar above has to match it.
            enterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
            exitTransition = { fadeOut(tween(TRANSITION_MILLIS)) }
        ) {
            composable<ReadingRoute> {
                FeedScreen(
                    onArticleClick = { navController.navigate(ArticleDetailRoute(it)) }
                )
            }
            composable<SavedRoute> {
                SavedScreen(
                    onArticleClick = { navController.navigate(ArticleDetailRoute(it)) }
                )
            }
            composable<ArticleDetailRoute> { entry ->
                // Read here only to prove the typed route survives the argument encoding.
                entry.toRoute<ArticleDetailRoute>()
                ArticleDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** How long a destination takes to arrive or leave, and therefore how long the bar takes. */
private const val TRANSITION_MILLIS = 700

/**
 * Tapping the current tab must not stack another copy of it, and leaving a tab must keep
 * its state for when the user comes back. Both are requirements in the app-shell spec.
 */
private fun androidx.navigation.NavHostController.switchTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
