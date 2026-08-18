package com.github.iscle.haven.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.iscle.haven.ui.theme.HavenAccent
import com.github.iscle.haven.ui.theme.HavenGlassBorder
import com.github.iscle.haven.ui.theme.HavenGlassBorderFaint
import com.github.iscle.haven.ui.theme.HavenGlassFill
import com.github.iscle.haven.ui.theme.HavenGlassHighlight
import com.github.iscle.haven.ui.theme.HavenInk
import com.github.iscle.haven.ui.theme.HavenOnSurface
import com.github.iscle.haven.ui.theme.HavenOnSurfaceFaint
import com.github.iscle.haven.ui.theme.HavenOnSurfaceMuted
import com.github.iscle.haven.ui.theme.RajdhaniFontFamily

/**
 * A pane that sits on top of the photo. The design calls for backdrop blur;
 * Android only offers that from API 31 and only for windows, so the translation
 * is a translucent dark fill plus a hairline highlight border, which keeps the
 * same contrast against a bright or a dark photo.
 */
fun Modifier.glass(
    shape: Shape,
    fill: Color = HavenGlassFill,
    border: Color = HavenGlassBorder
): Modifier = this
    .clip(shape)
    .background(fill)
    .border(1.dp, border, shape)

fun Modifier.glass(
    cornerRadius: Dp,
    fill: Color = HavenGlassFill,
    border: Color = HavenGlassBorder
): Modifier = glass(RoundedCornerShape(cornerRadius), fill, border)

/** A card on the flat dark system (settings, drawer, history chrome). */
fun Modifier.havenCard(cornerRadius: Dp = 20.dp): Modifier = glass(
    shape = RoundedCornerShape(cornerRadius),
    fill = Color.White.copy(alpha = 0.045f),
    border = Color.White.copy(alpha = 0.08f)
)

/** Round glass button — 60dp on the photo, 56dp on the flat system. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    iconSize: Dp = 30.dp,
    emphasized: Boolean = false,
    tint: Color = if (emphasized) Color.White else Color.White.copy(alpha = 0.92f)
) {
    Box(
        modifier = modifier
            .size(size)
            .glass(
                shape = CircleShape,
                fill = if (emphasized) HavenGlassHighlight else HavenGlassFill,
                border = if (emphasized) Color.White.copy(alpha = 0.22f) else HavenGlassBorder
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Circular button on the flat dark system. */
@Composable
fun FlatIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .glass(
                shape = CircleShape,
                fill = Color.White.copy(alpha = 0.06f),
                border = Color.White.copy(alpha = 0.09f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = HavenOnSurface,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** Small caps section header used across settings, history and the drawer. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = HavenOnSurfaceFaint
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.4.sp,
        fontFamily = RajdhaniFontFamily,
        color = color
    )
}

/** 68x38 pill switch with a 28dp thumb, per the design. */
@Composable
fun HavenSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 68.dp, height = 38.dp)
            .clip(CircleShape)
            .background(if (checked) HavenAccent else Color.White.copy(alpha = 0.16f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 5.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (checked) HavenInk else Color.White.copy(alpha = 0.7f))
        )
    }
}

/** Two-or-more option pill group (All/Favorites, °C/°F). */
@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 26.dp,
    verticalPadding: Dp = 10.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp
) {
    Row(
        modifier = modifier
            .glass(
                shape = CircleShape,
                fill = Color.White.copy(alpha = 0.06f),
                border = Color.White.copy(alpha = 0.09f)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (selected) Color.White.copy(alpha = 0.9f) else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            ) {
                Text(
                    text = option,
                    fontSize = fontSize,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = RajdhaniFontFamily,
                    color = if (selected) HavenInk else HavenOnSurfaceMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/** Filled action button (light on dark), e.g. "Save", "Show on clock now". */
@Composable
fun HavenFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 64.dp
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(horizontal = 34.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HavenInk,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = RajdhaniFontFamily,
            color = HavenInk
        )
    }
}

/** Outlined action button, e.g. "View on Unsplash". */
@Composable
fun HavenOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 64.dp,
    border: BorderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
    contentColor: Color = HavenOnSurface,
    dashed: Boolean = false
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = border.width,
                brush = border.brush,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            fontSize = if (dashed) 21.sp else 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = RajdhaniFontFamily,
            color = contentColor
        )
    }
}

/**
 * One row inside a settings card: title, supporting line, trailing control.
 * Rows are separated by a hairline, the last row has none.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    verticalPadding: Dp = 22.dp,
    trailing: @Composable () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = RajdhaniFontFamily,
                    color = HavenOnSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 18.sp,
                        fontFamily = RajdhaniFontFamily,
                        color = HavenOnSurfaceFaint,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing()
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.07f))
            )
        }
    }
}
