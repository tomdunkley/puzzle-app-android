package com.tomdunkley.dailypuzzles.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PestControlRodent
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomdunkley.dailypuzzles.R

// Base avatars available to all users. Mirrors VALID_AVATAR_IDS in user_service.py.
val AVATAR_IDS = listOf("person")

// Achievement-unlocked avatars (shown locked until earned).
val ACHIEVEMENT_AVATAR_IDS = listOf(
    "numbers", "words", "friend", "flex", "taunt", "owl", "eight", "meta", "queen", "santa",
    "sunglasses", "bullseye",
    "fire", "bolt_icon", "star_icon", "shield", "coffee", "anchor",
    "heart", "music", "snowflake", "sun_icon", "lotus", "pizza",
    "cake", "egg", "raven",
    "route", "map", "compass", "trail", "globe", "mountain", "wave", "mouse", "tree", "car", "train",
)

// Base colors (always available). Gold and black are unlocked via achievements.
val AVATAR_COLOR_IDS = listOf("red", "green", "blue", "orange")
const val GOLD_COLOR_ID = "gold"
const val BLACK_COLOR_ID = "black"

// Icon colours (the tint applied to the icon itself, not the background).
// White and black are always available; silver is unlocked via the completionist achievement.
const val SILVER_ICON_COLOR_ID = "silver"

fun iconFor(avatarId: String?): ImageVector = when (avatarId) {
    "person" -> Icons.Filled.Person
    "numbers" -> Icons.Filled.Calculate
    "words" -> Icons.Filled.GridOn
    "friend" -> Icons.Filled.Groups
    "flex" -> Icons.Filled.FitnessCenter
    "meta" -> Icons.Filled.AllInclusive
    // Streak icons
    "fire" -> Icons.Filled.Whatshot
    "bolt_icon" -> Icons.Filled.Bolt
    "star_icon" -> Icons.Filled.Star
    "shield" -> Icons.Filled.Shield
    "coffee" -> Icons.Filled.LocalCafe
    "anchor" -> Icons.Filled.Anchor
    "heart" -> Icons.Filled.Favorite
    "music" -> Icons.Filled.MusicNote
    "snowflake" -> Icons.Filled.AcUnit
    "sun_icon" -> Icons.Filled.WbSunny
    "lotus" -> Icons.Filled.Spa
    "pizza" -> Icons.Filled.LocalPizza
    "cake" -> Icons.Filled.Cake
    // Routes icons
    "route" -> Icons.Filled.Route
    "map" -> Icons.Filled.Map
    "compass" -> Icons.Filled.Explore
    "trail" -> Icons.Filled.Hiking
    "globe" -> Icons.Filled.Public
    "mountain" -> Icons.Filled.Terrain
    "wave" -> Icons.Filled.Waves
    "mouse" -> Icons.Filled.PestControlRodent
    "tree" -> Icons.Filled.Park
    "car" -> Icons.Filled.DirectionsCar
    "train" -> Icons.Filled.Train
    else -> Icons.Filled.Person
}

fun colorFor(avatarColorId: String?): Color? = when (avatarColorId) {
    "red" -> Color(0xFFD32F2F)
    "green" -> Color(0xFF388E3C)
    "blue" -> Color(0xFF1565C0)
    "orange" -> Color(0xFFE65100)
    "gold" -> Color(0xFFFFAA00)
    "black" -> Color(0xFF212121)
    "silver" -> Color(0xFFC0C0C0)
    "purple" -> Color(0xFF7B1FA2)
    "teal" -> Color(0xFF00695C)
    "pink" -> Color(0xFFE91E63)
    "lime" -> Color(0xFF558B2F)
    else -> null
}

private fun lighten(color: Color, amount: Float = 0.35f): Color = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
    alpha = color.alpha,
)

/** Two-tone diagonal swatch: dark top-left triangle, lighter bottom-right. */
@Composable
fun DiagonalSwatch(baseColor: Color, modifier: Modifier = Modifier) {
    val lightColor = lighten(baseColor)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                close()
            },
            color = lightColor,
        )
        drawPath(
            path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            color = baseColor,
        )
    }
}

@Composable
private fun SelectedBadge(size: Dp) {
    Box(
        modifier = Modifier.size(size * 0.4f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = "Selected",
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(size * 0.4f),
        )
    }
}

@Composable
fun AvatarIcon(
    avatarId: String?,
    avatarColorId: String? = null,
    avatarIconColor: String? = null,
    selected: Boolean = false,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val chosenColor = colorFor(avatarColorId)
    val tintColor = when (avatarIconColor) {
        "black" -> Color.Black
        "white" -> Color.White
        "silver" -> Color(0xFFC0C0C0)
        else -> if (chosenColor != null) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    }

    val iconPainter: Painter = when (avatarId) {
        "taunt" -> painterResource(R.drawable.ic_taunt)
        "owl" -> painterResource(R.drawable.ic_owl)
        "eight" -> painterResource(R.drawable.ic_counter_8)
        "queen" -> painterResource(R.drawable.ic_crown)
        "santa" -> painterResource(R.drawable.ic_celebration)
        "sunglasses" -> painterResource(R.drawable.ic_sunglasses)
        "bullseye" -> painterResource(R.drawable.ic_bullseye)
        "egg" -> painterResource(R.drawable.ic_egg)
        "raven" -> painterResource(R.drawable.ic_raven)
        else -> rememberVectorPainter(iconFor(avatarId))
    }

    Box(modifier = modifier.size(size)) {
        if (chosenColor != null) {
            DiagonalSwatch(baseColor = chosenColor, modifier = Modifier.size(size))
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Icon(
            painter = iconPainter,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(size * 0.6f).align(Alignment.Center),
        )
        if (selected) {
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                SelectedBadge(size)
            }
        }
    }
}
