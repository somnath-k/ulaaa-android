package com.dotkios.ulaaa.ui.bucketlist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.runtime.Composable
import com.dotkios.ulaaa.ui.components.PlaceholderScreen

@Composable
fun BucketListScreen() {
    PlaceholderScreen(
        title = "Bucket List",
        subtitle = "Saved destinations, powered by Room, arrive in Sprint 4.",
        icon = Icons.Outlined.Bookmark,
    )
}
