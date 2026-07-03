package com.tomdunkley.dailypuzzles.ui.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthState
import com.tomdunkley.dailypuzzles.data.network.dto.FriendRequestSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.FriendSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.UserSearchResultDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.SignInPrompt

@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onGoToSettings: () -> Unit,
    onMutation: () -> Unit = {},
    viewModel: FriendsViewModel = viewModel(),
) {
    val authState by AuthRepository.state.collectAsState()

    Scaffold(
        topBar = { SectionTopBar(title = "Friends", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = authState) {
                is AuthState.SignedIn -> SignedInContent(state, viewModel, onMutation)
                else -> SignInPrompt("Sign in to view your friends.", onGoToSettings)
            }
        }
    }
}

@Composable
private fun SignedInContent(
    state: AuthState.SignedIn,
    viewModel: FriendsViewModel,
    onMutation: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingUnfriend by remember { mutableStateOf<FriendSummaryDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFriendsData()
        onMutation()
    }

    pendingUnfriend?.let { friend ->
        AlertDialog(
            onDismissRequest = { pendingUnfriend = null },
            title = { Text("Remove ${friend.displayName}?") },
            text = { Text("You'll need to send a new friend request to add them again.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unfriend(friend.userId)
                    pendingUnfriend = null
                }) { Text("REMOVE") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnfriend = null }) { Text("CANCEL") }
            },
        )
    }

    when (val loaded = uiState as? FriendsUiState.Loaded) {
        null -> CenteredFriendsMessage(uiState)
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text("Signed in as ${state.displayName}", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(
                    value = loaded.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    label = { Text("Find friends by email or name") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            if (loaded.searchQuery.isNotBlank()) {
                item { SectionLabel("Search results") }
                if (loaded.isSearching) {
                    item { CircularProgressIndicator() }
                } else if (loaded.searchResults.isEmpty()) {
                    item {
                        Text(
                            "No one found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(loaded.searchResults, key = { it.userId }) { result ->
                        SearchResultRow(result, onSendRequest = { viewModel.sendRequest(result.userId) })
                    }
                }
                item { HorizontalDivider() }
            }
            if (loaded.incomingRequests.isNotEmpty()) {
                item { SectionLabel("Friend requests") }
                items(loaded.incomingRequests, key = { it.userId }) { request ->
                    IncomingRequestRow(
                        request,
                        onAccept = { viewModel.acceptRequest(request.userId); onMutation() },
                        onDecline = { viewModel.declineRequest(request.userId); onMutation() },
                    )
                }
                item { HorizontalDivider() }
            }
            item { SectionLabel("Your friends") }
            if (loaded.friends.isEmpty()) {
                item {
                    Text(
                        "No friends yet -- search above to add some.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(loaded.friends, key = { it.userId }) { friend ->
                    FriendRow(friend, onRemove = { pendingUnfriend = friend })
                }
            }
        }
    }
}

@Composable
private fun CenteredFriendsMessage(uiState: FriendsUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        when (uiState) {
            is FriendsUiState.Loading -> CircularProgressIndicator()
            is FriendsUiState.Error -> Text(uiState.message, style = MaterialTheme.typography.bodyLarge)
            else -> Unit
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SearchResultRow(result: UserSearchResultDto, onSendRequest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarIcon(result.avatarId, result.avatarColorId, avatarIconColor = result.avatarIconColor, size = 32.dp)
            Text(result.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
        }
        when (result.friendshipStatus) {
            "friends" -> Text("Friends", color = MaterialTheme.colorScheme.onSurfaceVariant)
            "request_sent" -> Text("Requested", color = MaterialTheme.colorScheme.onSurfaceVariant)
            "request_received" -> Text("Check requests below", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> TextButton(onClick = onSendRequest) { Text("ADD") }
        }
    }
}

@Composable
private fun IncomingRequestRow(request: FriendRequestSummaryDto, onAccept: () -> Unit, onDecline: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarIcon(request.avatarId, request.avatarColorId, avatarIconColor = request.avatarIconColor, size = 32.dp)
            Text(request.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
        }
        Row {
            TextButton(onClick = onAccept) { Text("ACCEPT") }
            TextButton(onClick = onDecline) { Text("DECLINE") }
        }
    }
}

@Composable
private fun FriendRow(friend: FriendSummaryDto, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarIcon(friend.avatarId, friend.avatarColorId, avatarIconColor = friend.avatarIconColor, size = 32.dp)
            Text(friend.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove ${friend.displayName}")
        }
    }
}
