package com.tomdunkley.dailypuzzles.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Top bar used on every screen.
 * When backgroundColor is set (game screens), the Surface draws behind the status bar so
 * the accent color fills the entire top of the screen. Content starts below the status bar
 * via windowInsetsPadding(statusBars) on the inner Column.
 */
@Composable
fun SectionTopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    backgroundColor: Color? = null,
) {
    val resolvedNavIcon: @Composable (() -> Unit)? = when {
        onBack != null -> ({
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        })
        else -> navigationIcon
    }
    val bg = backgroundColor ?: MaterialTheme.colorScheme.surface
    Surface(color = bg) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (resolvedNavIcon != null) {
                    Row(modifier = Modifier.padding(end = 4.dp)) { resolvedNavIcon() }
                    if (subtitle != null) {
                        Column(modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 8.dp)) {
                            Text(text = title, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                } else {
                    Text(
                        text = "td",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface))
                            .padding(horizontal = 10.dp, vertical = 2.dp),
                    )
                    Text(
                        text = " $title",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
