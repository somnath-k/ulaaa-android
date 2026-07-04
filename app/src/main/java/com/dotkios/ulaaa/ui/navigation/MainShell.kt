package com.dotkios.ulaaa.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dotkios.ulaaa.ui.bucketlist.BucketListScreen
import com.dotkios.ulaaa.ui.home.HomeScreen
import com.dotkios.ulaaa.ui.map.MapScreen
import com.dotkios.ulaaa.ui.profile.ProfileScreen

/** Authenticated shell: bottom-nav Scaffold hosting the top-level destinations. */
@Composable
fun MainShell(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            UlaaaBottomBar(
                currentRoute = currentRoute,
                onSelect = { dest ->
                    navController.navigate(dest.route) {
                        // Single top + restore state = standard bottom-nav behaviour.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) { HomeScreen() }
            composable(TopLevelDestination.MAP.route) { MapScreen() }
            composable(TopLevelDestination.BUCKET_LIST.route) { BucketListScreen() }
            composable(TopLevelDestination.PROFILE.route) { ProfileScreen(onSignOut = onSignOut) }
        }
    }
}
