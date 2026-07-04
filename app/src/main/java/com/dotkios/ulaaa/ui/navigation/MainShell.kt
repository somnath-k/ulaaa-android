package com.dotkios.ulaaa.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dotkios.ulaaa.ui.bucketlist.BucketListScreen
import com.dotkios.ulaaa.ui.home.HomeScreen
import com.dotkios.ulaaa.ui.map.MapScreen
import com.dotkios.ulaaa.ui.profile.ProfileScreen
import com.dotkios.ulaaa.ui.trip.CreateTripScreen
import com.dotkios.ulaaa.ui.trip.TripsScreen

private const val ROUTE_TRIPS = "trips"
private const val ROUTE_TRIP_CREATE = "trip_create"

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
                onSelect = { dest -> navController.selectTab(dest) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onSeeAllTrips = { navController.navigate(ROUTE_TRIPS) },
                    onCreateTrip = { navController.navigate(ROUTE_TRIP_CREATE) },
                    onOpenMap = { navController.selectTab(TopLevelDestination.MAP) },
                )
            }
            composable(TopLevelDestination.MAP.route) { MapScreen() }
            composable(TopLevelDestination.BUCKET_LIST.route) { BucketListScreen() }
            composable(TopLevelDestination.PROFILE.route) { ProfileScreen(onSignOut = onSignOut) }

            composable(ROUTE_TRIPS) {
                TripsScreen(
                    onBack = { navController.popBackStack() },
                    onCreateTrip = { navController.navigate(ROUTE_TRIP_CREATE) },
                )
            }
            composable(ROUTE_TRIP_CREATE) {
                CreateTripScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Standard bottom-nav behaviour: single top + save/restore state. */
private fun NavHostController.selectTab(dest: TopLevelDestination) {
    navigate(dest.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
