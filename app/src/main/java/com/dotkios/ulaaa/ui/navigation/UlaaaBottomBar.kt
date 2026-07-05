package com.dotkios.ulaaa.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dotkios.ulaaa.ui.components.bounceClick

/** Flat bar with icon-only tabs and a raised center "+" to create a trip. */
@Composable
fun UlaaaBottomBar(
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
    onCreate: () -> Unit,
) {
    val tabs = TopLevelDestination.entries
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 10.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(tabs[0], currentRoute, onSelect)
            NavItem(tabs[1], currentRoute, onSelect)
            CreateButton(onCreate)
            NavItem(tabs[2], currentRoute, onSelect)
            NavItem(tabs[3], currentRoute, onSelect)
        }
    }
}

@Composable
private fun RowScope.NavItem(
    dest: TopLevelDestination,
    currentRoute: String?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val selected = currentRoute == dest.route
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .weight(1f)
            .bounceClick { onSelect(dest) },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
            contentDescription = dest.label,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun RowScope.CreateButton(onCreate: () -> Unit) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .bounceClick(onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Create trip",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
