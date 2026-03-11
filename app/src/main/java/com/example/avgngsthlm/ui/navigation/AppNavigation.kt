package com.example.avgngsthlm.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

sealed class Screen(val route: String) {
    object Favorites : Screen("favorites")
    object AddFavorite : Screen("add_favorite")
    object EditFavorite : Screen("edit_favorite/{favoriteId}") {
        fun createRoute(id: Int) = "edit_favorite/$id"
    }
    object AutoMode : Screen("auto_mode")
    object Departures : Screen("departures")
    object Help : Screen("help")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Departures, "Avgångar", Icons.Default.Home),
        BottomNavItem(Screen.Favorites, "Favoriter", Icons.Default.Star),
        BottomNavItem(Screen.AutoMode, "Auto mode", Icons.Default.AccessTime)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.screen.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.screen.route,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Departures.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Favorites.route) {
                com.example.avgngsthlm.ui.screens.favorites.FavoritesScreen(
                    onAddFavorite = { navController.navigate(Screen.AddFavorite.route) },
                    onHelp = { navController.navigate(Screen.Help.route) },
                    onEditFavorite = { id -> navController.navigate(Screen.EditFavorite.createRoute(id)) }
                )
            }
            composable(
                Screen.EditFavorite.route,
                arguments = listOf(navArgument("favoriteId") { type = NavType.IntType })
            ) {
                com.example.avgngsthlm.ui.screens.editfavorite.EditFavoriteScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AddFavorite.route) {
                com.example.avgngsthlm.ui.screens.addfavorite.AddFavoriteScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AutoMode.route) {
                com.example.avgngsthlm.ui.screens.automode.AutoModeScreen()
            }
            composable(Screen.Departures.route) {
                com.example.avgngsthlm.ui.screens.departures.DeparturesScreen()
            }
            composable(Screen.Help.route) {
                com.example.avgngsthlm.ui.screens.help.HelpScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
