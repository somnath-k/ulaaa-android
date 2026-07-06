package com.dotkios.ulaaa.ui.map

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.data.model.FriendLocation
import com.dotkios.ulaaa.data.model.GeoPoint
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.ui.components.AppButton
import com.dotkios.ulaaa.ui.components.CardImage
import com.dotkios.ulaaa.ui.components.LandmarkCard
import com.dotkios.ulaaa.ui.components.PlaceholderScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val OLA_STYLE_URL =
    "https://api.olamaps.io/tiles/vector/v1/styles/default-light-standard/style.json"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(permission.status.isGranted) {
        if (permission.status.isGranted) viewModel.loadNearby()
    }

    if (!permission.status.isGranted) {
        LocationPermissionPrompt(onGrant = { permission.launchPermissionRequest() })
        return
    }

    var selected by remember { mutableStateOf<Landmark?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OlaMap(
            location = state.location,
            landmarks = state.landmarks,
            friends = state.friends,
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            )
        }

        state.error?.let { error ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                onClick = { viewModel.loadNearby() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            ) {
                Text(
                    text = "$error  Tap to retry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }

        if (state.landmarks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Nearby landmarks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.landmarks, key = { it.id }) { landmark ->
                        LandmarkCard(landmark = landmark, onClick = { selected = landmark })
                    }
                }
            }
        }
    }

    selected?.let { landmark ->
        LandmarkDetailDialog(landmark = landmark, onDismiss = { selected = null })
    }
}

@Composable
private fun LandmarkDetailDialog(landmark: Landmark, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column {
                CardImage(
                    imageUrl = landmark.imageUrl,
                    accent = landmark.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = landmark.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${landmark.category} · ${landmark.distanceKm} km away",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = landmark.description
                            ?: "No description available for this place yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Close") }
                }
            }
        }
    }
}

private const val SOURCE_PLACES = "places-src"
private const val SOURCE_FRIENDS = "friends-src"
private const val SOURCE_ME = "me-src"

@Composable
private fun OlaMap(
    location: GeoPoint?,
    landmarks: List<Landmark>,
    friends: List<FriendLocation>,
    modifier: Modifier = Modifier,
) {
    val mapView = rememberMapViewWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { libreMap ->
                    libreMap.setStyle(Style.Builder().fromUri(OLA_STYLE_URL)) { loaded ->
                        setupMarkerLayers(loaded)
                        map = libreMap
                        style = loaded
                    }
                }
            }
        },
    )

    LaunchedEffect(location, map) {
        val libreMap = map ?: return@LaunchedEffect
        val point = location ?: return@LaunchedEffect
        libreMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(point.lat, point.lon))
            .zoom(13.5)
            .build()
    }

    LaunchedEffect(location, style) {
        val loaded = style ?: return@LaunchedEffect
        val point = location ?: return@LaunchedEffect
        val me = Feature.fromGeometry(Point.fromLngLat(point.lon, point.lat))
            .apply { addStringProperty("title", "You") }
        (loaded.getSource(SOURCE_ME) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(listOf(me)))
    }

    LaunchedEffect(landmarks, style) {
        val loaded = style ?: return@LaunchedEffect
        val features = landmarks.mapNotNull { lm ->
            val lat = lm.lat ?: return@mapNotNull null
            val lon = lm.lon ?: return@mapNotNull null
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply { addStringProperty("title", lm.name) }
        }
        (loaded.getSource(SOURCE_PLACES) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(friends, style) {
        val loaded = style ?: return@LaunchedEffect
        val features = friends.map { f ->
            Feature.fromGeometry(Point.fromLngLat(f.lon, f.lat)).apply { addStringProperty("title", f.name) }
        }
        (loaded.getSource(SOURCE_FRIENDS) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}

/** Adds empty GeoJSON sources + circle/label layers for landmark and friend pins. */
private fun setupMarkerLayers(style: Style) {
    val teal = 0xFF0E7C7B.toInt()
    val coral = 0xFFFF6F5E.toInt()
    val white = 0xFFFFFFFF.toInt()
    val dark = 0xFF15201F.toInt()

    style.addSource(GeoJsonSource(SOURCE_PLACES))
    style.addLayer(
        CircleLayer("places-circle", SOURCE_PLACES).withProperties(
            PropertyFactory.circleColor(teal),
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleStrokeColor(white),
            PropertyFactory.circleStrokeWidth(2f),
        ),
    )
    style.addLayer(
        SymbolLayer("places-label", SOURCE_PLACES).withProperties(
            PropertyFactory.textField(Expression.get("title")),
            PropertyFactory.textSize(11f),
            PropertyFactory.textColor(dark),
            PropertyFactory.textHaloColor(white),
            PropertyFactory.textHaloWidth(1.2f),
            PropertyFactory.textOffset(arrayOf(0f, 1.4f)),
            PropertyFactory.textAllowOverlap(false),
        ),
    )

    style.addSource(GeoJsonSource(SOURCE_FRIENDS))
    style.addLayer(
        CircleLayer("friends-circle", SOURCE_FRIENDS).withProperties(
            PropertyFactory.circleColor(coral),
            PropertyFactory.circleRadius(9f),
            PropertyFactory.circleStrokeColor(white),
            PropertyFactory.circleStrokeWidth(3f),
        ),
    )
    style.addLayer(
        SymbolLayer("friends-label", SOURCE_FRIENDS).withProperties(
            PropertyFactory.textField(Expression.get("title")),
            PropertyFactory.textSize(11f),
            PropertyFactory.textColor(coral),
            PropertyFactory.textHaloColor(white),
            PropertyFactory.textHaloWidth(1.2f),
            PropertyFactory.textOffset(arrayOf(0f, -1.6f)),
            PropertyFactory.textAllowOverlap(true),
        ),
    )

    // "You" — the user's own live location.
    style.addSource(GeoJsonSource(SOURCE_ME))
    style.addLayer(
        CircleLayer("me-circle", SOURCE_ME).withProperties(
            PropertyFactory.circleColor(0xFF1E6FEA.toInt()),
            PropertyFactory.circleRadius(9f),
            PropertyFactory.circleStrokeColor(white),
            PropertyFactory.circleStrokeWidth(4f),
        ),
    )
    style.addLayer(
        SymbolLayer("me-label", SOURCE_ME).withProperties(
            PropertyFactory.textField(Expression.get("title")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor(0xFF1E6FEA.toInt()),
            PropertyFactory.textHaloColor(white),
            PropertyFactory.textHaloWidth(1.4f),
            PropertyFactory.textOffset(arrayOf(0f, -1.6f)),
            PropertyFactory.textAllowOverlap(true),
        ),
    )
}

@Composable
private fun LocationPermissionPrompt(onGrant: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        PlaceholderScreen(
            title = "Discover nearby",
            subtitle = "Ulaaa needs your location to show landmarks around you.",
            icon = Icons.Outlined.Place,
        )
        AppButton(
            text = "Enable location",
            onClick = onGrant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        )
    }
}
