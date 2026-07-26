package com.dotkios.ulaaa.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dotkios.ulaaa.ui.bucketlist.BucketListScreen
import com.dotkios.ulaaa.ui.chat.ChatScreen
import com.dotkios.ulaaa.ui.curated.CuratedDetailScreen
import com.dotkios.ulaaa.ui.feed.FeedScreen
import com.dotkios.ulaaa.ui.friends.FriendsScreen
import com.dotkios.ulaaa.ui.friends.InviteContactsScreen
import com.dotkios.ulaaa.ui.home.HomeScreen
import com.dotkios.ulaaa.ui.map.MapScreen
import com.dotkios.ulaaa.ui.profile.ProfileScreen
import com.dotkios.ulaaa.ui.trip.CreateTripScreen
import com.dotkios.ulaaa.ui.trip.TripDetailScreen
import com.dotkios.ulaaa.ui.trip.TripsScreen
import com.dotkios.ulaaa.ui.tripchat.TripChatScreen

private const val ROUTE_TRIP_CREATE = "trip_create"
private const val ROUTE_CHAT = "chat"
private const val ROUTE_TRIP_DETAIL = "trip_detail"
private const val ROUTE_TRIP_CHAT = "trip_chat"
private const val ROUTE_FRIENDS = "friends"
private const val ROUTE_INVITE_CONTACTS = "invite_contacts"
private const val ROUTE_BUCKET_LIST = "bucketlist"
private const val ROUTE_CURATED = "curated_detail"

/** Authenticated shell: bottom-nav Scaffold hosting the top-level destinations. */
@Composable
fun MainShell(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Floating nav overlays content (no reserved white band); shown only on tab screens.
    val onTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.START.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(TopLevelDestination.HOME.route) {
                HomeScreen(
                    onSeeAllTrips = { navController.selectTab(TopLevelDestination.TRIPS) },
                    onCreateTrip = { navController.navigate(ROUTE_TRIP_CREATE) },
                    onOpenMap = { navController.selectTab(TopLevelDestination.MAP) },
                    onAskDot = { prompt ->
                        val route = if (prompt.isNullOrBlank()) {
                            ROUTE_CHAT
                        } else {
                            "$ROUTE_CHAT?prompt=${Uri.encode(prompt)}"
                        }
                        navController.navigate(route)
                    },
                    onOpenCurated = { c ->
                        navController.navigate(
                            "$ROUTE_CURATED?title=${Uri.encode(c.title)}" +
                                "&destination=${Uri.encode(c.destination)}&days=${c.days}",
                        )
                    },
                )
            }
            composable(TopLevelDestination.FEED.route) { FeedScreen() }
            composable(TopLevelDestination.TRIPS.route) {
                TripsScreen(
                    onCreateTrip = { navController.navigate(ROUTE_TRIP_CREATE) },
                    onOpenTrip = { id -> navController.navigate("$ROUTE_TRIP_DETAIL/$id") },
                )
            }
            composable(TopLevelDestination.MAP.route) { MapScreen() }
            composable(TopLevelDestination.PROFILE.route) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onOpenFriends = { navController.navigate(ROUTE_FRIENDS) },
                    onOpenBucketList = { navController.navigate(ROUTE_BUCKET_LIST) },
                )
            }
            composable(ROUTE_BUCKET_LIST) {
                BucketListScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "$ROUTE_CURATED?title={title}&destination={destination}&days={days}",
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("destination") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("days") { type = NavType.StringType; nullable = true; defaultValue = "3" },
                ),
            ) {
                CuratedDetailScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { tripId ->
                        navController.navigate("$ROUTE_TRIP_DETAIL/$tripId") {
                            popUpTo(TopLevelDestination.HOME.route)
                        }
                    },
                )
            }

            composable(
                route = "$ROUTE_TRIP_DETAIL/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            ) { entry ->
                val tripId = entry.arguments?.getString("tripId").orEmpty()
                TripDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChat = { navController.navigate("$ROUTE_TRIP_CHAT/$tripId") },
                )
            }
            composable(
                route = "$ROUTE_TRIP_CHAT/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            ) {
                TripChatScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_TRIP_CREATE) {
                CreateTripScreen(
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "$ROUTE_CHAT?prompt={prompt}",
                arguments = listOf(
                    navArgument("prompt") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                ChatScreen(onBack = { navController.popBackStack() })
            }
            composable(ROUTE_FRIENDS) {
                FriendsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenInviteContacts = { navController.navigate(ROUTE_INVITE_CONTACTS) },
                )
            }
            composable(ROUTE_INVITE_CONTACTS) {
                InviteContactsScreen(onBack = { navController.popBackStack() })
            }
        }

        if (onTopLevel) {
            UlaaaBottomBar(
                currentRoute = currentRoute,
                onSelect = { dest -> navController.selectTab(dest) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
