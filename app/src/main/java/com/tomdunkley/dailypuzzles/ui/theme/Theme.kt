package com.tomdunkley.dailypuzzles.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PrintInk,
    onPrimary = PrintNewsprint,
    secondary = PrintGrey,
    onSecondary = PrintNewsprint,
    tertiary = PrintGreyLight,
    onTertiary = PrintInk,
    background = PrintNewsprint,
    onBackground = PrintInk,
    surface = PrintNewsprint,
    onSurface = PrintInk,
    surfaceVariant = PrintNewsprintDim,
    onSurfaceVariant = PrintInkLight,
    outline = PrintRule,
    outlineVariant = PrintRule,
)

private val DarkColors = darkColorScheme(
    primary = PrintPaperOnDark,
    onPrimary = PrintInkDarkBg,
    secondary = PrintGreyLight,
    onSecondary = PrintInkDarkBg,
    tertiary = PrintGrey,
    onTertiary = PrintPaperOnDark,
    background = PrintInkDarkBg,
    onBackground = PrintPaperOnDark,
    surface = PrintInkDarkSurface,
    onSurface = PrintPaperOnDark,
    surfaceVariant = PrintInkDarkSurface,
    onSurfaceVariant = PrintGreyLight,
    outline = PrintGrey,
    outlineVariant = PrintGrey,
)

@Composable
fun DailyPuzzlesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
