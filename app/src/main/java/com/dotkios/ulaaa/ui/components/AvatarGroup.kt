package com.dotkios.ulaaa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val AvatarPalette = listOf(0xFF0E7C7B, 0xFFFF6B57, 0xFFF4A259, 0xFF5A6FEA)

/** Overlapping avatar stack used to hint a trip's Squad size. */
@Composable
fun AvatarGroup(
    count: Int,
    modifier: Modifier = Modifier,
    max: Int = 3,
    avatarSize: Int = 28,
) {
    val shown = count.coerceAtMost(max)
    val overflow = count - shown
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(shown) { index ->
            Bubble(
                size = avatarSize,
                bg = AvatarPalette[index % AvatarPalette.size],
                label = ('A' + index).toString(),
                overlap = index > 0,
            )
        }
        if (overflow > 0) {
            Bubble(
                size = avatarSize,
                bg = 0xFF3F4949,
                label = "+$overflow",
                overlap = shown > 0,
            )
        }
    }
}

@Composable
private fun Bubble(size: Int, bg: Long, label: String, overlap: Boolean) {
    Box(
        modifier = Modifier
            .offset(x = if (overlap) (-(size / 3)).dp else 0.dp)
            .size(size.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color(bg))
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = androidx.compose.ui.graphics.Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}
