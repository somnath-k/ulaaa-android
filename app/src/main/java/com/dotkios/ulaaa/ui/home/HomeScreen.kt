package com.dotkios.ulaaa.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.ui.components.CategoryChip
import com.dotkios.ulaaa.ui.components.bounceClick
import com.dotkios.ulaaa.ui.components.CuratedCard
import com.dotkios.ulaaa.ui.components.LandmarkCard
import com.dotkios.ulaaa.ui.components.SectionTitle
import com.dotkios.ulaaa.ui.components.TripCard

private val HeaderTop = Color(0xFF0E7C7B)
private val HeaderBottom = Color(0xFF0A5A59)

@Composable
fun HomeScreen(
    onSeeAllTrips: () -> Unit,
    onCreateTrip: () -> Unit,
    onOpenMap: () -> Unit,
    onAskDot: (starterPrompt: String?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onSeeAllTrips = onSeeAllTrips,
        onCreateTrip = onCreateTrip,
        onOpenMap = onOpenMap,
        onAskDot = onAskDot,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onSeeAllTrips: () -> Unit,
    onCreateTrip: () -> Unit,
    onOpenMap: () -> Unit,
    onAskDot: (String?) -> Unit,
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
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { HomeHeader(name = state.userName, onOpenChat = { onAskDot(null) }) }

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
            Section(title = "Categories") {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.categories.forEach { category ->
                        CategoryChip(
                            category = category,
                            onClick = {
                                onAskDot(
                                    "Suggest the best ${category.label} destinations to visit in India. " +
                                        "Give 5 with a one-line reason each.",
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(name: String, onOpenChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Hello, $name 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "Where to next?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
        )
        AiSearchPill(onClick = onOpenChat, modifier = Modifier.padding(top = 14.dp))
    }
}

@Composable
private fun AiSearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
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
