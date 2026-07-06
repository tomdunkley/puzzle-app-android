package com.tomdunkley.dailypuzzles.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.R
import com.tomdunkley.dailypuzzles.data.network.dto.AchievementItemDto
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore
import com.tomdunkley.dailypuzzles.ui.components.DiagonalSwatch
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.SignInPrompt
import com.tomdunkley.dailypuzzles.ui.components.colorFor
import com.tomdunkley.dailypuzzles.ui.components.iconFor

@Composable
fun AchievementsScreen(
    isSignedIn: Boolean,
    onSignInClick: () -> Unit,
    viewModel: AchievementsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { if (isSignedIn) viewModel.load() }

    DisposableEffect(Unit) {
        onDispose { TrophySeenStore.markAllSeen() }
    }

    Scaffold(
        topBar = { SectionTopBar(title = "Trophies") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        if (!isSignedIn) {
            SignInPrompt("Sign in to see your trophies.", onSignInClick, Modifier.padding(padding))
            return@Scaffold
        }
        when (val state = uiState) {
            is AchievementsUiState.Loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { CircularProgressIndicator() }

            is AchievementsUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) { Text(state.message, style = MaterialTheme.typography.bodyLarge) }

            is AchievementsUiState.Loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                // Sort: completed (newest first) → incomplete non-streak → words streak → numbers streak
                fun group(item: AchievementItemDto): Int = when {
                    item.unlocked -> 0
                    !item.id.contains("streak") -> 1
                    item.id.startsWith("words_streak") -> 2
                    else -> 3 // numbers_streak
                }
                val sorted = state.achievements.sortedWith { a, b ->
                    val ga = group(a)
                    val gb = group(b)
                    when {
                        ga != gb -> ga - gb
                        ga == 0 -> (b.unlockedAt ?: "").compareTo(a.unlockedAt ?: "")
                        else -> 0
                    }
                }
                items(sorted, key = { it.id }) { achievement ->
                    AchievementRow(achievement, isNew = achievement.id in state.newTrophyIds)
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(achievement: AchievementItemDto, isNew: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (achievement.unlocked) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BadgedBox(badge = { if (isNew) Badge(modifier = Modifier.size(12.dp)) }) {
            AchievementBadge(achievement)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (achievement.unlocked) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (achievement.unlocked) achievement.description else "?????",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (achievement.unlocked) {
                achievement.unlockedAt?.let { iso ->
                    Text(
                        text = "Unlocked ${formatDate(iso)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatDate(iso: String): String = try {
    val year = iso.substring(0, 4)
    val month = when (iso.substring(5, 7)) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
        else -> iso.substring(5, 7)
    }
    val day = iso.substring(8, 10).trimStart('0')
    "$day $month $year"
} catch (_: Exception) { "" }

@Composable
private fun AchievementBadge(achievement: AchievementItemDto) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.5.dp, MaterialTheme.colorScheme.onSurface),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !achievement.unlocked -> {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }

            achievement.unlocksIconColorId != null -> {
                // Icon colour unlock — small centered swatch to distinguish from background colours
                val swatchColor = colorFor(achievement.unlocksIconColorId) ?: Color(0xFFC0C0C0)
                DiagonalSwatch(baseColor = swatchColor, modifier = Modifier.size(32.dp))
            }

            achievement.unlocksColorId != null -> {
                val swatchColor = colorFor(achievement.unlocksColorId)
                if (swatchColor != null) {
                    DiagonalSwatch(baseColor = swatchColor, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFC0C0C0)))
                }
            }

            achievement.unlocksAvatarId != null -> {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
                val painter = when (achievement.unlocksAvatarId) {
                    "taunt" -> painterResource(R.drawable.ic_taunt)
                    "owl" -> painterResource(R.drawable.ic_owl)
                    "eight" -> painterResource(R.drawable.ic_counter_8)
                    "queen" -> painterResource(R.drawable.ic_crown)
                    "santa" -> painterResource(R.drawable.ic_celebration)
                    "sunglasses" -> painterResource(R.drawable.ic_sunglasses)
                    "bullseye" -> painterResource(R.drawable.ic_bullseye)
                    "egg" -> painterResource(R.drawable.ic_egg)
                    "raven" -> painterResource(R.drawable.ic_raven)
                    else -> rememberVectorPainter(iconFor(achievement.unlocksAvatarId))
                }
                Icon(
                    painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface))
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
