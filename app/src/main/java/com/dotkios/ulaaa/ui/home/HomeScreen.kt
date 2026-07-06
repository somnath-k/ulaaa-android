package com.dotkios.ulaaa.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.ui.components.CategoryChip
import com.dotkios.ulaaa.ui.components.CuratedCard
import com.dotkios.ulaaa.ui.components.LandmarkCard
import com.dotkios.ulaaa.ui.components.SectionTitle
import com.dotkios.ulaaa.ui.components.TripCard
import com.dotkios.ulaaa.ui.components.bounceClick

@Composable
fun HomeScreen(
    onSeeAllTrips: () -> Unit,
    onCreateTrip: () -> Unit,
    onOpenMap: () -> Unit,
    onAskDot: (starterPrompt: String?) -> Unit,
    onOpenCurated: (com.dotkios.ulaaa.data.model.CuratedItinerary) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onSeeAllTrips = onSeeAllTrips,
        onCreateTrip = onCreateTrip,
        onOpenMap = onOpenMap,
        onAskDot = onAskDot,
        onOpenCurated = onOpenCurated,
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
    onOpenCurated: (com.dotkios.ulaaa.data.model.CuratedItinerary) -> Unit,
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
                when {
                    state.nearbyLoading -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        userScrollEnabled = false,
                    ) {
                        items(3) { LandmarkSkeleton() }
                    }

                    state.nearbyLandmarks.isEmpty() -> Text(
                        "No nearby spots found. Check your connection and reopen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )

                    else -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.nearbyLandmarks, key = { it.id }) { landmark ->
                            LandmarkCard(landmark = landmark, onClick = { onOpenMap() })
                        }
                    }
                }
            }
        }

        item {
            Section(title = "Curated For You") {
                when {
                    state.curatedLoading -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        userScrollEnabled = false,
                    ) {
                        items(2) { CuratedSkeleton() }
                    }

                    else -> LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(state.curated, key = { it.id }) { curated ->
                            CuratedCard(itinerary = curated, onClick = { onOpenCurated(curated) })
                        }
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
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(11.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Hey, $name 👋",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Where to next?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AiSearchPill(onClick = onOpenChat)
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
private fun LandmarkSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha",
    )
    val shade = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    Column(modifier = Modifier.width(160.dp)) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(shade),
        )
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .width(110.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shade),
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .width(72.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shade),
        )
    }
}

@Composable
private fun CuratedSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmerC")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alphaC",
    )
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
    )
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
