package com.tomdunkley.dailypuzzles.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.BuildConfig
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthState
import com.tomdunkley.dailypuzzles.data.auth.GoogleSignInHelper
import com.tomdunkley.dailypuzzles.data.challenges.PendingChallengesStore
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onForgotPasswordClick: () -> Unit,
    onEditAvatarClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onViewProfileClick: (userId: String) -> Unit = {},
    hasPendingFriendRequests: Boolean = false,
    viewModel: SettingsViewModel = viewModel(),
) {
    val authState by AuthRepository.state.collectAsState()

    Scaffold(
        topBar = { SectionTopBar(title = "Profile") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (authState) {
                is AuthState.SignedIn -> SignedInSettings(viewModel, onEditAvatarClick, onAccountSettingsClick, onFriendsClick, onViewProfileClick, hasPendingFriendRequests)
                else -> SignInContent(onForgotPasswordClick)
            }
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SignInContent(onForgotPasswordClick: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "Sign in to play and compare scores with friends.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            onClick = {
                isSubmitting = true
                errorMessage = null
                coroutineScope.launch {
                    val result = if (isRegisterMode) {
                        AuthRepository.register(email, password)
                    } else {
                        AuthRepository.login(email, password)
                    }
                    isSubmitting = false
                    result.onFailure { errorMessage = it.toUserMessage() }
                }
            },
        ) {
            Text(if (isRegisterMode) "CREATE ACCOUNT" else "SIGN IN")
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(
                if (isRegisterMode) "Already have an account? Sign in" else "Don't have an account? Register",
            )
        }
        if (!isRegisterMode) {
            TextButton(onClick = onForgotPasswordClick) {
                Text("Forgot password?")
            }
        }

        HorizontalDivider()

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            onClick = {
                if (!GoogleSignInHelper.isConfigured) {
                    errorMessage = "Sign in with Google isn't set up yet"
                    return@OutlinedButton
                }
                coroutineScope.launch {
                    runCatching { GoogleSignInHelper.requestIdToken(context) }
                        .onSuccess { idToken ->
                            AuthRepository.signInWithGoogle(idToken)
                                .onFailure { errorMessage = it.toUserMessage("Google sign-in failed") }
                        }
                        .onFailure { error ->
                            // The user dismissed the account picker -- not a real error,
                            // nothing to show.
                            if (error !is GetCredentialCancellationException) {
                                errorMessage = if (error is NoCredentialException) {
                                    "No Google account found on this device. Add one in Settings, or sign in with email above."
                                } else {
                                    error.toUserMessage("Google sign-in failed")
                                }
                            }
                        }
                }
            },
        ) {
            Text("SIGN IN WITH GOOGLE")
        }

        if (BuildConfig.DEBUG) {
            var devName by remember { mutableStateOf("") }
            HorizontalDivider()
            OutlinedTextField(
                value = devName,
                onValueChange = { devName = it },
                label = { Text("Dev login (display name)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                enabled = devName.isNotBlank() && !isSubmitting,
                onClick = {
                    isSubmitting = true
                    errorMessage = null
                    coroutineScope.launch {
                        AuthRepository.devLogin(devName.trim())
                            .onFailure { errorMessage = it.toUserMessage("Dev login failed") }
                        isSubmitting = false
                    }
                },
            ) {
                Text("DEV SIGN IN")
            }
        }
    }
}

@Composable
private fun SignedInSettings(
    viewModel: SettingsViewModel,
    onEditAvatarClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onFriendsClick: () -> Unit,
    onViewProfileClick: (userId: String) -> Unit,
    hasPendingFriendRequests: Boolean,
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingChallengeCount by PendingChallengesStore.pendingCount.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    when (val state = uiState) {
        is SettingsUiState.Loading -> CenteredContent { CircularProgressIndicator() }
        is SettingsUiState.Error -> CenteredMessage(state.message)
        is SettingsUiState.Loaded -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AvatarIcon(
                        avatarId = state.profile.avatarId,
                        avatarColorId = state.profile.avatarColorId,
                        avatarIconColor = state.profile.avatarIconColor,
                        size = 64.dp,
                    )
                    Text(
                        text = state.profile.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onEditAvatarClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                    }
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    onClick = { onViewProfileClick(state.profile.userId) },
                ) {
                    Text("VIEW PROFILE")
                }

                BadgedBox(
                    badge = { if (hasPendingFriendRequests || pendingChallengeCount > 0) Badge(modifier = Modifier.size(10.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        onClick = onFriendsClick,
                    ) {
                        Text("FRIENDS")
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    onClick = onAccountSettingsClick,
                ) {
                    Text("SETTINGS")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    onClick = { AuthRepository.signOut() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("SIGN OUT")
                }
            }
        }
    }
}

@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onChangePasswordClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    Scaffold(
        topBar = { SectionTopBar(title = "Settings", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        val uiState by viewModel.uiState.collectAsState()
        LaunchedEffect(Unit) { viewModel.load() }

        when (val state = uiState) {
            is SettingsUiState.Loading -> Column(Modifier.padding(padding)) { CenteredContent { CircularProgressIndicator() } }
            is SettingsUiState.Error -> Column(Modifier.padding(padding)) { CenteredMessage(state.message) }
            is SettingsUiState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    state.profile.email?.let { email ->
                        Text("Signed in as $email", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !state.isSaving,
                                onClick = { viewModel.updateVisibleOnGlobalLeaderboard(!state.profile.visibleOnGlobalLeaderboard) },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = state.profile.visibleOnGlobalLeaderboard,
                            onCheckedChange = { viewModel.updateVisibleOnGlobalLeaderboard(it) },
                            enabled = !state.isSaving,
                        )
                        Text("Visible on global rankings", style = MaterialTheme.typography.bodyLarge)
                    }

                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider()
                    Text("ACCOUNT", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (state.profile.hasPassword) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                            onClick = onChangePasswordClick,
                        ) {
                            Text("CHANGE PASSWORD")
                        }
                    } else {
                        SetPasswordSection(state, viewModel)
                    }
                    if (!state.profile.hasGoogle) {
                        LinkGoogleSection(state, viewModel)
                    }

                    var showDeleteDialog by remember { mutableStateOf(false) }
                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete account") },
                            text = { Text("This will permanently delete your account and all your data, including scores and trophies. This cannot be undone.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteDialog = false
                                        viewModel.deleteAccount()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    ),
                                ) { Text("DELETE") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("CANCEL") }
                            },
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        enabled = !state.isSaving,
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("DELETE ACCOUNT")
                    }

                    if (state.profile.isDeveloper) {
                        HorizontalDivider()
                        Text("DEVELOPER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                            enabled = !state.isSaving,
                            onClick = { viewModel.resetDevProgress() },
                        ) {
                            Text("RESET MY PUZZLES")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                            onClick = { viewModel.resetPracticeHighScores() },
                        ) {
                            Text("RESET UNLIMITED HIGH SCORES")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                            enabled = !state.isSaving,
                            onClick = { viewModel.unlockAllAchievements() },
                        ) {
                            Text("UNLOCK ALL TROPHIES")
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                            enabled = !state.isSaving,
                            onClick = { viewModel.resetAchievements() },
                        ) {
                            Text("RESET MY TROPHIES")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetPasswordSection(state: SettingsUiState.Loaded, viewModel: SettingsViewModel) {
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Set up a password so you can sign in without Google too.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isSaving,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && newPassword.length >= 8,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            onClick = { viewModel.setPassword(newPassword) },
        ) {
            Text("SET PASSWORD")
        }
    }
}

@Composable
private fun LinkGoogleSection(state: SettingsUiState.Loaded, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Link a Google account so you can sign in that way too.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            enabled = !state.isSaving,
            onClick = {
                if (!GoogleSignInHelper.isConfigured) {
                    viewModel.reportError("Sign in with Google isn't set up yet")
                    return@OutlinedButton
                }
                coroutineScope.launch {
                    runCatching { GoogleSignInHelper.requestIdToken(context) }
                        .onSuccess { idToken -> viewModel.linkGoogleAccount(idToken) }
                        .onFailure { error ->
                            // The user dismissed the account picker -- not a real error,
                            // nothing to show.
                            if (error !is GetCredentialCancellationException) {
                                viewModel.reportError(
                                    if (error is NoCredentialException) {
                                        "No Google account found on this device."
                                    } else {
                                        error.toUserMessage("Couldn't link Google account")
                                    },
                                )
                            }
                        }
                }
            },
        ) {
            Text("LINK GOOGLE ACCOUNT")
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun CenteredMessage(message: String) {
    CenteredContent {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
