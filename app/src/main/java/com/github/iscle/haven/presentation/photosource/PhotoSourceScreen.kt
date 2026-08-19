package com.github.iscle.haven.presentation.photosource

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.iscle.haven.domain.model.PhotoSource
import com.github.iscle.haven.domain.model.PhotoSourceType
import com.github.iscle.haven.presentation.common.FlatIconButton
import com.github.iscle.haven.presentation.common.HavenFilledButton
import com.github.iscle.haven.presentation.common.HavenMotion
import com.github.iscle.haven.presentation.common.HavenSwitch
import com.github.iscle.haven.presentation.common.SegmentedToggle
import com.github.iscle.haven.presentation.common.glass
import com.github.iscle.haven.ui.theme.HavenMint
import com.github.iscle.haven.ui.theme.HavenOnSurface
import com.github.iscle.haven.ui.theme.HavenOnSurfaceDim
import com.github.iscle.haven.ui.theme.HavenOnSurfaceFaint
import com.github.iscle.haven.ui.theme.HavenOnSurfaceMuted
import com.github.iscle.haven.ui.theme.HavenSurface

/** 2I — every place Haven draws wallpapers from. */
@Composable
fun PhotoSourceScreen(
    onNavigateBack: () -> Unit,
    viewModel: PhotoSourceViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // SAF, not the photo picker: only SAF grants survive a reboot, and a
    // wallpaper source has to keep working tomorrow.
    val pickPhotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModel.addDevicePhotos(uris.map { it.toString() })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HavenSurface)
            .safeDrawingPadding()
    ) {
        Header(
            showCredit = uiState.preferences.showPhotographerCredit,
            onCreditChange = viewModel::setShowPhotographerCredit,
            tab = uiState.tab,
            onSelectTab = viewModel::selectTab,
            onNavigateBack = onNavigateBack
        )

        // The input, the chips and the cards scroll as one, so nothing is
        // permanently clipped on a 800px panel.
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 44.dp),
            contentPadding = PaddingValues(top = 18.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.tab == PhotoSourceTab.UNSPLASH) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AddSourceRow(
                        input = uiState.input,
                        isAdding = uiState.isAdding,
                        onInputChange = viewModel::onInputChange,
                        onAdd = viewModel::addFromInput
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SuggestionChips(onPick = viewModel::addSuggestion)
                }
            }

            val error = uiState.errorMessage
            if (error != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(text = error, fontSize = 18.sp, color = HavenOnSurfaceMuted)
                }
            }

            items(items = uiState.visibleSources, key = { it.id }) { source ->
                SourceCard(
                    modifier = Modifier.animateItem(),
                    source = source,
                    isActive = source.id == uiState.activeSourceId,
                    onToggle = { enabled -> viewModel.setEnabled(source.id, enabled) },
                    onRemove = { viewModel.remove(source.id) }
                )
            }
            item {
                AddDevicePhotosTile(onClick = { pickPhotos.launch(arrayOf("image/*")) })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 44.dp, top = 16.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Photos load in landscape crop, filtered to 1280×800 or larger",
                fontSize = 18.sp,
                color = HavenOnSurface.copy(alpha = 0.4f)
            )
            Text(
                text = "${uiState.enabledCount} ${if (uiState.enabledCount == 1) "source" else "sources"} on",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HavenOnSurfaceMuted
            )
        }
    }
}

@Composable
private fun Header(
    showCredit: Boolean,
    onCreditChange: (Boolean) -> Unit,
    tab: PhotoSourceTab,
    onSelectTab: (PhotoSourceTab) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, end = 44.dp, top = 28.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FlatIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onNavigateBack
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Photo source",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HavenOnSurface
                )
                Text(
                    text = "Haven shuffles across everything switched on here",
                    fontSize = 19.sp,
                    color = HavenOnSurfaceFaint
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = "Photographer credit",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = HavenOnSurfaceMuted
                )
                HavenSwitch(checked = showCredit, onCheckedChange = onCreditChange)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            SegmentedToggle(
                options = listOf("Unsplash", "On device"),
                selectedIndex = if (tab == PhotoSourceTab.UNSPLASH) 0 else 1,
                onSelect = { index ->
                    onSelectTab(
                        if (index == 0) PhotoSourceTab.UNSPLASH else PhotoSourceTab.DEVICE
                    )
                },
                horizontalPadding = 22.dp,
                verticalPadding = 9.dp,
                fontSize = 19.sp
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.07f))
        )
    }
}

@Composable
private fun AddSourceRow(
    input: String,
    isAdding: Boolean,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .glass(
                    shape = RoundedCornerShape(14.dp),
                    fill = Color.Black.copy(alpha = 0.22f),
                    border = Color.White.copy(alpha = 0.2f)
                )
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = HavenOnSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (input.isEmpty()) {
                    Text(
                        text = "Add a query or paste a collection URL",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = HavenOnSurface.copy(alpha = 0.42f)
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White.copy(alpha = 0.7f)),
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = HavenOnSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        HavenFilledButton(
            text = if (isAdding) "Adding…" else "Add",
            onClick = onAdd,
            height = 58.dp
        )
    }
}

@Composable
private fun SuggestionChips(onPick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PHOTO_SOURCE_SUGGESTIONS.forEach { suggestion ->
            Box(
                modifier = Modifier
                    .glass(
                        shape = CircleShape,
                        fill = Color.Transparent,
                        border = Color.White.copy(alpha = 0.14f)
                    )
                    .clickable { onPick(suggestion) }
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    text = suggestion,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = HavenOnSurfaceMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SourceCard(
    source: PhotoSource,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A source that is switched off fades back rather than jumping.
    val dimmed by animateFloatAsState(
        targetValue = if (source.enabled) 1f else 0.6f,
        animationSpec = tween(HavenMotion.CONTROL_MILLIS),
        label = "source_dim"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .glass(
                shape = RoundedCornerShape(20.dp),
                fill = Color.White.copy(alpha = if (source.enabled) 0.05f else 0.03f),
                border = Color.White.copy(alpha = if (source.enabled) 0.16f else 0.08f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val preview = source.previewUrl
            if (preview.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4B4550).copy(alpha = dimmed),
                                    Color(0xFF17151A)
                                )
                            )
                        )
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(preview)
                        .crossfade(HavenMotion.SCREEN_MILLIS)
                        .build(),
                    contentDescription = source.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = dimmed
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive && source.enabled,
                enter = fadeIn(tween(HavenMotion.SCREEN_MILLIS)) + scaleIn(initialScale = 0.9f),
                exit = fadeOut(tween(HavenMotion.CONTROL_MILLIS)),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = HavenMint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Active now",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Seeded sources stay put; anything the user added can be removed.
            if (source.id != "default-query" && source.id != "favorites") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${source.label}",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (source.type == PhotoSourceType.UNSPLASH_QUERY) {
                        "\"${source.label}\""
                    } else {
                        source.label
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HavenOnSurface.copy(alpha = if (source.enabled) 1f else 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.subtitle.orEmpty(),
                    fontSize = 17.sp,
                    color = HavenOnSurfaceFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HavenSwitch(checked = source.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun AddDevicePhotosTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .height(164.dp)
            .clip(RoundedCornerShape(20.dp))
            .glass(
                shape = RoundedCornerShape(20.dp),
                fill = Color.Transparent,
                border = Color.White.copy(alpha = 0.2f)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddPhotoAlternate,
            contentDescription = null,
            tint = HavenOnSurfaceMuted,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Add photos from this device",
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            color = HavenOnSurfaceMuted
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Kept across restarts",
            fontSize = 16.sp,
            color = HavenOnSurfaceDim
        )
    }
}
