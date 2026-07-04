package com.dotkios.ulaaa.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.ui.components.CategoryChip
import com.dotkios.ulaaa.ui.components.CuratedCard
import com.dotkios.ulaaa.ui.components.LandmarkCard
import com.dotkios.ulaaa.ui.components.SectionTitle
import com.dotkios.ulaaa.ui.components.TripCard

@Composable
fun HomeScreen(
    onSeeAllTrips: () -> Unit,
    onCreateTrip: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenChat: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onSeeAllTrips = onSeeAllTrips,
        onCreateTrip = onCreateTrip,
        onOpenMap = onOpenMap,
        onOpenChat = onOpenChat,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onSeeAllTrips: () -> Unit,
    onCreateTrip: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenChat: () -> Unit,
) {
    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Greeting(name = state.userName) }

        item {
            AiSearchEntry(onClick = onOpenChat)
        }

        item {
            Section(
                title = "Upcoming Trips",
                actionLabel = if (state.trips.isEmpty()) null else "See all",
                onAction = onSeeAllTrips,
            ) {
                if (state.trips.isEmpty()) {
                    CreateTripPrompt(onCreateTrip = onCreateTrip)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.trips, key = { it.id }) { trip ->
                            TripCard(trip = trip, onClick = { onSeeAllTrips() })
                        }
                    }
                }
            }
        }

        item {
            Section(title = "Curated For You", actionLabel = "See all") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.curated, key = { it.id }) { curated ->
                        CuratedCard(itinerary = curated, onClick = {})
                    }
                }
            }
        }

        item {
            Section(title = "Nearby Landmarks", actionLabel = "Map", onAction = onOpenMap) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(state.nearbyLandmarks, key = { it.id }) { landmark ->
                        LandmarkCard(landmark = landmark, onClick = { onOpenMap() })
                    }
                }
            }
        }

        item {
            Section(title = "Categories") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.categories.forEach { category ->
                        CategoryChip(category = category, onClick = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSearchEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Ask Dot anything…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateTripPrompt(onCreateTrip: () -> Unit) {
    Card(
        onClick = onCreateTrip,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Plan your first trip",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "Tap to create a trip and invite your Squad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun Greeting(name: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "Hello, $name 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Where to next?",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Section(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(
            title = title,
            actionLabel = actionLabel,
            onActionClick = if (actionLabel != null) (onAction ?: {}) else null,
        )
        content()
    }
}
