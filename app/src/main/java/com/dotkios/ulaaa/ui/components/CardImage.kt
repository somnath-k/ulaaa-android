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
import com.dotkios.ulaaa.util.destinationImageUrl

/**
 * Card hero image: a real travel photo (by keyword) layered over the item's accent gradient,
 * which stays visible while the photo loads or if it fails. Optional bottom scrim keeps
 * overlaid text legible. [content] is drawn on top for captions/badges.
 */
@Composable
fun CardImage(
    keywords: String,
    accent: Color,
    modifier: Modifier = Modifier,
    seed: String = keywords,
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
        val model = if (!imageUrl.isNullOrBlank()) imageUrl else destinationImageUrl(keywords, seed)
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
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
