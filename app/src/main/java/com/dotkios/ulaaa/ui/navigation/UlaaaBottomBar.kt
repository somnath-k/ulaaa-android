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
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(34.dp),
            color = PillDark,
            shadowElevation = 14.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillItem(tabs[0], currentRoute, onSelect)
                PillItem(tabs[1], currentRoute, onSelect)
                CreatePill(onCreate)
                PillItem(tabs[2], currentRoute, onSelect)
                PillItem(tabs[3], currentRoute, onSelect)
            }
        }
    }
}

@Composable
private fun PillItem(
    dest: TopLevelDestination,
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val selected = currentRoute == dest.route
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .bounceClick { onSelect(dest) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else PillIconIdle,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun CreatePill(onCreate: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary)
            .bounceClick(onCreate),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = "Create trip",
            tint = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.size(26.dp),
        )
    }
}
