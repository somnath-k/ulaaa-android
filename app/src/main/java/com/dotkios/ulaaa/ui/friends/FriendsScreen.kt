package com.dotkios.ulaaa.ui.friends

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dotkios.ulaaa.data.model.Friend
import com.dotkios.ulaaa.data.model.FriendRequest
import com.dotkios.ulaaa.data.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val friends by viewModel.friends.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join me on Ulaaa — let's plan a trip together! 🌍",
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Invite a friend"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Invite")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AddFriend(
                    email = email,
                    onEmailChange = { email = it },
                    isSearching = search.isSearching,
                    onSearch = { viewModel.search(email) },
                    result = search.result,
                    message = search.message,
                    onSendRequest = { viewModel.sendRequest(it) },
                )
            }

            if (requests.isNotEmpty()) {
                item { SectionHeader("Requests (${requests.size})") }
                items(requests, key = { it.fromUid }) { request ->
                    RequestRow(
                        request = request,
                        onAccept = { viewModel.accept(request) },
                        onDecline = { viewModel.decline(request.fromUid) },
                    )
                }
            }

            item { SectionHeader("My friends (${friends.size})") }
            if (friends.isEmpty()) {
                item {
                    Text(
                        "No friends yet. Search by email or share an invite.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(friends, key = { it.uid }) { friend ->
                    FriendRow(friend = friend, onRemove = { viewModel.remove(friend.uid) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun AddFriend(
    email: String,
    onEmailChange: (String) -> Unit,
    isSearching: Boolean,
    onSearch: () -> Unit,
    result: UserProfile?,
    message: String?,
    onSendRequest: (UserProfile) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                modifier = Modifier.weight(1f),
                label = { Text("Friend's email") },
                singleLine = true,
            )
            IconButton(onClick = onSearch, enabled = !isSearching && email.isNotBlank()) {
                Icon(Icons.Filled.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        result?.let { profile ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(profile.name)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    ) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(profile.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalButton(onClick = { onSendRequest(profile) }) { Text("Add") }
                }
            }
        }
    }
}

@Composable
private fun RequestRow(request: FriendRequest, onAccept: () -> Unit, onDecline: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(request.name)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(request.name.ifBlank { request.email }, style = MaterialTheme.typography.titleMedium)
                Text("wants to be friends", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onAccept) {
                Icon(Icons.Filled.Check, contentDescription = "Accept", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDecline) {
                Icon(Icons.Filled.Close, contentDescription = "Decline", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FriendRow(friend: Friend, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(friend.name)
            Text(
                friend.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            TextButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@Composable
private fun Avatar(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1).uppercase().ifBlank { "?" },
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
