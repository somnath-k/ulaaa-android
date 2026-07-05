package com.dotkios.ulaaa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Card hero: shows the real place photo ([imageUrl], e.g. from Wikipedia) when we have one,
 * otherwise a clean brand gradient — never a random stock photo. Optional bottom [scrim] keeps
 * overlaid text legible; [content] is drawn on top for captions/badges.
 */
@Composable
fun CardImage(
    accent: Color,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    scrim: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.6f)))),
        )
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (scrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
                        ),
                    ),
            )
        }
        content()
    }
}
