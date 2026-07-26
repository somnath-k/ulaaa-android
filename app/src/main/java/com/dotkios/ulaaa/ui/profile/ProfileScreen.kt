package com.dotkios.ulaaa.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dotkios.ulaaa.data.model.Post

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenBucketList: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPhoneDialog by remember { mutableStateOf(false) }
    var pendingPostImage by remember { mutableStateOf<Uri?>(null) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::changePhoto) }

    val postPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> pendingPostImage = uri }

    val imageRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 110.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ProfileHeader(
                state = state,
                onEditAvatar = { avatarPicker.launch(imageRequest) },
                onEditPhone = { showPhoneDialog = true },
                onOpenFriends = onOpenFriends,
                onOpenBucketList = onOpenBucketList,
                onSignOut = {
                    viewModel.signOut()
                    onSignOut()
                },
                onAddPost = { postPicker.launch(imageRequest) },
            )
        }

        if (state.posts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "No posts yet. Tap “Add” to share your first trip photo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            items(state.posts, key = { it.id }) { post ->
                PostCell(post = post, onClick = { selectedPost = post })
            }
        }
    }

    if (showPhoneDialog) {
        PhoneDialog(
            initial = state.phone,
            onDismiss = { showPhoneDialog = false },
            onSave = {
                viewModel.setPhone(it)
                showPhoneDialog = false
            },
        )
    }

    pendingPostImage?.let { uri ->
        NewPostDialog(
            image = uri,
            posting = state.posting,
            onDismiss = { pendingPostImage = null },
            onPost = { caption ->
                viewModel.addPost(uri, caption)
                pendingPostImage = null
            },
        )
    }

    selectedPost?.let { post ->
        AlertDialog(
            onDismissRequest = { selectedPost = null },
            title = { Text("Delete post?") },
            text = { Text("This photo will be removed from your profile.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost(post)
                    selectedPost = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { selectedPost = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileUiState,
    onEditAvatar: () -> Unit,
    onEditPhone: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenBucketList: () -> Unit,
    onSignOut: () -> Unit,
    onAddPost: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onEditAvatar),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.uploadingPhoto -> CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    state.photoUrl.isNotBlank() -> AsyncImage(
                        model = state.photoUrl,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> Text(
                        text = state.name.take(1).uppercase(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Surface(
                onClick = onEditAvatar,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                shadowElevation = 3.dp,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Change photo",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(7.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = state.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (state.email.isNotBlank()) {
            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onEditPhone) {
            Text(if (state.phone.isBlank()) "Add phone number" else "📱 ${state.phone}  ·  Edit")
        }

        state.error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onOpenFriends, modifier = Modifier.weight(1f)) { Text("Friends") }
            OutlinedButton(onClick = onOpenBucketList, modifier = Modifier.weight(1f)) { Text("Bucket List") }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign Out") }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "My Posts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(onClick = onAddPost, enabled = !state.posting) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (state.posting) "Posting…" else "Add")
            }
        }
    }
}

@Composable
private fun PostCell(post: Post, onClick: () -> Unit) {
    AsyncImage(
        model = post.imageUrl,
        contentDescription = post.caption.ifBlank { "Post" },
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun NewPostDialog(
    image: Uri,
    posting: Boolean,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
) {
    var caption by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!posting) onDismiss() },
        title = { Text("New post") },
        text = {
            Column {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPost(caption) }, enabled = !posting) {
                Text(if (posting) "Posting…" else "Share")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !posting) { Text("Cancel") } },
    )
}

@Composable
private fun PhoneDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your phone number") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Phone") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
