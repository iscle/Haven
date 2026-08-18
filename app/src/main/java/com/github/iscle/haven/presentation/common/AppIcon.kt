package com.github.iscle.haven.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.github.iscle.haven.ui.theme.HavenOnSurfaceFaint

private const val DEFAULT_ICON_PIXELS = 144

/** Decodes at the size it is drawn at — the allow-list renders a lot of these. */
@Composable
fun rememberAppIcon(packageName: String, sizePixels: Int = DEFAULT_ICON_PIXELS): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName, sizePixels) {
        runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(sizePixels, sizePixels)
                .asImageBitmap()
        }.getOrNull()
    }
}

/** App icon on a rounded tile, with a neutral fallback when the icon is missing. */
@Composable
fun AppIconTile(
    packageName: String,
    label: String,
    modifier: Modifier = Modifier,
    tileSize: Dp = 104.dp,
    iconSize: Dp = 72.dp,
    cornerRadius: Dp = 30.dp,
    tint: Color = Color.White.copy(alpha = 0.05f)
) {
    val iconPixels = with(LocalDensity.current) { iconSize.roundToPx() }
    val icon = rememberAppIcon(packageName, iconPixels)
    Box(
        modifier = modifier
            .size(tileSize)
            .glass(
                shape = RoundedCornerShape(cornerRadius),
                fill = tint,
                border = Color.White.copy(alpha = 0.08f)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = label,
                modifier = Modifier.size(iconSize)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = label,
                tint = HavenOnSurfaceFaint,
                modifier = Modifier.size(iconSize / 2)
            )
        }
    }
}
