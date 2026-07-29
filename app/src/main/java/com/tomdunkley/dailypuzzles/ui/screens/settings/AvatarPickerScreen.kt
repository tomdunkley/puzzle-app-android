package com.tomdunkley.dailypuzzles.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.tomdunkley.dailypuzzles.ui.components.ACHIEVEMENT_AVATAR_IDS
import com.tomdunkley.dailypuzzles.ui.components.AVATAR_COLOR_IDS
import com.tomdunkley.dailypuzzles.ui.components.AVATAR_IDS
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.DiagonalSwatch
import com.tomdunkley.dailypuzzles.ui.components.BLACK_COLOR_ID
import com.tomdunkley.dailypuzzles.ui.components.GOLD_COLOR_ID
import com.tomdunkley.dailypuzzles.ui.components.SILVER_ICON_COLOR_ID
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.colorFor

@Composable
fun AvatarPickerScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Edit profile",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val state = uiState) {
            is SettingsUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) { CircularProgressIndicator() }
            is SettingsUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) { Text(state.message, style = MaterialTheme.typography.bodyLarge) }
            is SettingsUiState.Loaded -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                var displayNameInput by remember(state.profile.displayName) { mutableStateOf(state.profile.displayName) }

                Text("DISPLAY NAME", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = displayNameInput,
                    onValueChange = { displayNameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isSaving,
                )
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving &&
                        displayNameInput.isNotBlank() &&
                        displayNameInput != state.profile.displayName,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                    onClick = { viewModel.updateDisplayName(displayNameInput) },
                ) {
                    Text("SAVE NAME")
                }

                HorizontalDivider()

                Text("ICON", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val unlockedAvatars = state.unlockedAchievementAvatarIds
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val availableAvatars = AVATAR_IDS + ACHIEVEMENT_AVATAR_IDS.filter { unlockedAvatars.contains(it) }
                    val cols = 4
                    val cellSize = (maxWidth - 36.dp) / cols
                    val avatarRows = (availableAvatars.size + cols - 1) / cols
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.fillMaxWidth().height(cellSize * avatarRows + 12.dp * (avatarRows - 1)),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(availableAvatars) { avatarId ->
                            val isSelected = avatarId == state.profile.avatarId
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    .clickable(enabled = !state.isSaving) {
                                        viewModel.updateAvatar(avatarId)
                                    },
                            ) {
                                AvatarIcon(
                                    avatarId = avatarId,
                                    avatarColorId = state.profile.avatarColorId,
                                    avatarIconColor = state.profile.avatarIconColor,
                                    selected = isSelected,
                                    size = cellSize,
                                )
                            }
                        }
                    }
                }

                if (state.unlockedAchievementCount < 5) {
                    Text(
                        "Unlock more icons by earning trophies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider()

                Text("BACKGROUND COLOUR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val unlockedColors = state.unlockedAchievementColorIds
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val availableColors = AVATAR_COLOR_IDS +
                        (if (unlockedColors.contains(GOLD_COLOR_ID)) listOf(GOLD_COLOR_ID) else emptyList()) +
                        (if (unlockedColors.contains(BLACK_COLOR_ID)) listOf(BLACK_COLOR_ID) else emptyList()) +
                        (if (unlockedColors.contains("silver")) listOf("silver") else emptyList()) +
                        listOf("purple", "teal", "pink", "lime", "yellow", "sky", "indigo").filter { unlockedColors.contains(it) }
                    val cols = 4
                    val cellSize = (maxWidth - 36.dp) / cols
                    val colorRows = (availableColors.size + cols - 1) / cols
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.fillMaxWidth().height(cellSize * colorRows + 12.dp * (colorRows - 1)),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(availableColors) { colorId ->
                            val isSelected = colorId == state.profile.avatarColorId
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    .clickable(enabled = !state.isSaving) {
                                        viewModel.updateAvatarColor(colorId)
                                    },
                            ) {
                                DiagonalSwatch(
                                    baseColor = colorFor(colorId) ?: MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(cellSize),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(22.dp).align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text("ICON COLOUR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val currentIconColor = state.profile.avatarIconColor
                val effectiveIconColor = currentIconColor
                    ?: if (state.profile.avatarColorId != null) "white" else "black"
                val unlockedIconColors = state.unlockedAchievementIconColorIds
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val iconColorOptions = buildList {
                        add("white" to Color.White)
                        add("black" to Color.Black)
                        if (unlockedIconColors.contains(SILVER_ICON_COLOR_ID)) add("silver" to Color(0xFFC0C0C0))
                    }
                    val cols = 4
                    val cellSize = (maxWidth - 36.dp) / cols
                    val iconColorRows = (iconColorOptions.size + cols - 1) / cols
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(cols),
                        modifier = Modifier.fillMaxWidth().height(cellSize * iconColorRows + 12.dp * (iconColorRows - 1)),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(iconColorOptions) { (id, displayColor) ->
                            val isSelected = effectiveIconColor == id
                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .background(displayColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    .clickable(enabled = !state.isSaving) { viewModel.updateAvatarIconColor(id) },
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = if (id == "white") Color.Black else Color.White,
                                        modifier = Modifier.size(22.dp).align(Alignment.BottomEnd),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
