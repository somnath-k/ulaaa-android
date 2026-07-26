package com.dotkios.ulaaa.ui.curated

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.ui.components.AppButton

private fun rupees(value: Int) = "₹%,d".format(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuratedDetailScreen(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    viewModel: CuratedDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedTripId) {
        state.savedTripId?.let(onSaved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (!state.isLoading && state.stops.isNotEmpty()) {
                AppButton(
                    text = if (state.isSaving) "Saving…" else "Add to my trips",
                    onClick = viewModel::saveAsTrip,
                    enabled = !state.isSaving,
                    modifier = Modifier.padding(16.dp),
                )
            }
        },
    ) { padding ->
        val totalBudget = state.stops.sumOf { it.cost }
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    "Dot is planning ${state.destination}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            state.error != null -> Text(
                state.error!!,
                modifier = Modifier.padding(padding).padding(24.dp),
                color = MaterialTheme.colorScheme.error,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text("${state.days}-day trip to ${state.destination}", style = MaterialTheme.typography.titleMedium)
                    if (totalBudget > 0) {
                        Text(
                            "Estimated budget · ${rupees(totalBudget)} / person",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                state.stops.groupBy { it.day }.toSortedMap().forEach { (day, dayStops) ->
                    item {
                        Text(
                            "Day $day",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(dayStops) { stop -> StopRow(stop) }
                }
            }
        }
    }
}

@Composable
private fun StopRow(stop: ItineraryStop) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(stop.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (stop.detail.isNotBlank()) {
                Text(
                    stop.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (stop.cost > 0) {
            Text(
                rupees(stop.cost),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
