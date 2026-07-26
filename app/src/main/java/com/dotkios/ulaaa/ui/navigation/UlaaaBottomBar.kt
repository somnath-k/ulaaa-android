package com.dotkios.ulaaa.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dotkios.ulaaa.ui.components.bounceClick

// Floating pill palette — a deep, near-black green that reads on any screen.
private val PillDark = Color(0xFF15211E)
private val PillIconIdle = Color(0xFFB7C4BF)

/** Floating rounded pill nav: icon tabs with a green highlight and a center "+". */
@Composable
fun UlaaaBottomBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    onCreate: () -> Unit,
) {
    val tabs = TopLevelDestination.entries
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.background),
                ),
            )
            .navigationBarsPadding()
            .padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = PillDark,
            shadowElevation = 14.dp,
        ) {
            // Center "+" splits the tabs into two halves.
            val mid = tabs.size / 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.take(mid).forEach { PillItem(it, currentRoute, onSelect, Modifier.weight(1f)) }
                CreatePill(onCreate, Modifier.weight(1f))
                tabs.drop(mid).forEach { PillItem(it, currentRoute, onSelect, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PillItem(
    dest: TopLevelDestination,
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = currentRoute == dest.route
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .bounceClick { onSelect(dest) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                contentDescription = dest.label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else PillIconIdle,
                modifier = Modifier.size(23.dp),
            )
        }
    }
}

@Composable
private fun CreatePill(onCreate: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .bounceClick(onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Create trip",
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(25.dp),
            )
        }
    }
}
