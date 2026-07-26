package com.dotkios.ulaaa.ui.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Place
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
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
    // A tap on a nearby card requests the map to fly to that point; the nonce
    // makes repeated taps on the same card re-trigger the animation.
    var focusTarget by remember { mutableStateOf<GeoPoint?>(null) }
    var focusNonce by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        OlaMap(
            location = state.location,
            landmarks = state.landmarks,
            friends = state.friends,
            focusTarget = focusTarget,
            focusNonce = focusNonce,
            onPlaceTap = { name -> selected = state.landmarks.firstOrNull { it.name == name } },
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
                    .padding(bottom = 100.dp),
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
                        LandmarkCard(
                            landmark = landmark,
                            onClick = {
                                val lat = landmark.lat
                                val lon = landmark.lon
                                if (lat != null && lon != null) {
                                    focusTarget = GeoPoint(lat, lon)
                                    focusNonce++
                                }
                                selected = landmark
                            },
                        )
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
    val context = LocalContext.current
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
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Directions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { openDirections(context, landmark, "driving") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Drive")
                        }
                        OutlinedButton(
                            onClick = { openDirections(context, landmark, "walking") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Walk")
                        }
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("Close") }
                }
            }
        }
    }
}

/** Opens the device's maps app with a route to [landmark] in the given travel [mode]. */
private fun openDirections(context: Context, landmark: Landmark, mode: String) {
    val destination = if (landmark.lat != null && landmark.lon != null) {
        "${landmark.lat},${landmark.lon}"
    } else {
        Uri.encode(landmark.name)
    }
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=$mode",
    )
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private const val SOURCE_PLACES = "places-src"
private const val SOURCE_FRIENDS = "friends-src"
private const val SOURCE_ME = "me-src"

@Composable
private fun OlaMap(
    location: GeoPoint?,
    landmarks: List<Landmark>,
    friends: List<FriendLocation>,
    focusTarget: GeoPoint?,
    focusNonce: Int,
    onPlaceTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapView = rememberMapViewWithLifecycle()
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val currentOnPlaceTap by rememberUpdatedState(onPlaceTap)

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMapAsync { libreMap ->
                        libreMap.setStyle(Style.Builder().fromUri(OLA_STYLE_URL)) { loaded ->
                            setupMarkerLayers(loaded)
                            map = libreMap
                            style = loaded
                        }
                        // Tap a place pin -> open its detail.
                        libreMap.addOnMapClickListener { latLng ->
                            val screen = libreMap.projection.toScreenLocation(latLng)
                            val hitBox = RectF(screen.x - 40f, screen.y - 60f, screen.x + 40f, screen.y + 10f)
                            val title = libreMap.queryRenderedFeatures(hitBox, LAYER_PLACES)
                                .firstOrNull()?.getStringProperty("title")
                            if (title != null) {
                                currentOnPlaceTap(title)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            },
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ZoomButton(Icons.Filled.Add, "Zoom in") { map?.animateCamera(CameraUpdateFactory.zoomIn()) }
            ZoomButton(Icons.Filled.Remove, "Zoom out") { map?.animateCamera(CameraUpdateFactory.zoomOut()) }
        }
    }

    LaunchedEffect(location, map) {
        val libreMap = map ?: return@LaunchedEffect
        val point = location ?: return@LaunchedEffect
        libreMap.cameraPosition = CameraPosition.Builder()
            .target(LatLng(point.lat, point.lon))
            .zoom(13.5)
            .build()
    }

    LaunchedEffect(focusNonce) {
        val libreMap = map ?: return@LaunchedEffect
        val target = focusTarget ?: return@LaunchedEffect
        libreMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(target.lat, target.lon), 15.0),
        )
    }

    LaunchedEffect(location, style) {
        val loaded = style ?: return@LaunchedEffect
        val point = location ?: return@LaunchedEffect
        loaded.addImage("me-bubble", MapMarkers.initialBubble(context, "You", 0xFF1E6FEA.toInt()))
        val me = Feature.fromGeometry(Point.fromLngLat(point.lon, point.lat)).apply {
            addStringProperty("icon", "me-bubble")
            addStringProperty("title", "You")
        }
        (loaded.getSource(SOURCE_ME) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(listOf(me)))
    }

    LaunchedEffect(landmarks, style) {
        val loaded = style ?: return@LaunchedEffect
        val features = landmarks.mapNotNull { lm ->
            val lat = lm.lat ?: return@mapNotNull null
            val lon = lm.lon ?: return@mapNotNull null
            val iconId = "place-${lm.id}"
            val bubble = MapMarkers.photoBubble(context, lm.imageUrl)
                ?: MapMarkers.initialBubble(context, lm.name, 0xFF0E7C7B.toInt())
            loaded.addImage(iconId, bubble)
            Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addStringProperty("icon", iconId)
                addStringProperty("title", lm.name)
            }
        }
        (loaded.getSource(SOURCE_PLACES) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(friends, style) {
        val loaded = style ?: return@LaunchedEffect
        val features = friends.map { f ->
            val iconId = "friend-${f.uid}"
            loaded.addImage(iconId, MapMarkers.initialBubble(context, f.name, 0xFFFF6F5E.toInt()))
            Feature.fromGeometry(Point.fromLngLat(f.lon, f.lat)).apply {
                addStringProperty("icon", iconId)
                addStringProperty("title", f.name)
            }
        }
        (loaded.getSource(SOURCE_FRIENDS) as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}

@Composable
private fun ZoomButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.size(46.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(11.dp),
        )
    }
}

private const val LAYER_PLACES = "places-symbol"
private const val LAYER_FRIENDS = "friends-symbol"
private const val LAYER_ME = "me-symbol"

/** Adds empty GeoJSON sources + photo-bubble icon layers for places, friends and the user. */
private fun setupMarkerLayers(style: Style) {
    fun bubbleLayer(id: String, source: String) = SymbolLayer(id, source).withProperties(
        PropertyFactory.iconImage(Expression.get("icon")),
        PropertyFactory.iconSize(0.8f),
        PropertyFactory.iconAllowOverlap(true),
        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
    )
    style.addSource(GeoJsonSource(SOURCE_PLACES))
    style.addLayer(bubbleLayer(LAYER_PLACES, SOURCE_PLACES))
    style.addSource(GeoJsonSource(SOURCE_FRIENDS))
    style.addLayer(bubbleLayer(LAYER_FRIENDS, SOURCE_FRIENDS))
    style.addSource(GeoJsonSource(SOURCE_ME))
    style.addLayer(bubbleLayer(LAYER_ME, SOURCE_ME))
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
