package com.arflix.tv

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arflix.tv.ui.navigation.NavRoutes
import com.arflix.tv.ui.screens.search.SearchScreen
import com.arflix.tv.ui.screens.settings.SettingsScreen

@Composable
fun ArvioApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SEARCH
    ) {
        composable(NavRoutes.SEARCH) {
            SearchScreen(
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) }
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
