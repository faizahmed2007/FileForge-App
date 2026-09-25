package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EngineUtils
import com.example.ui.components.AsyncProcessingQueueCard
import com.example.ui.components.EngineStatusBanner
import com.example.ui.theme.MonoFont
import com.example.ui.viewmodel.FileForgeViewModel
import com.example.ui.viewmodel.FileItem
import com.example.ui.viewmodel.QueueStatus

@Composable
fun ConvertScreen(
    viewModel: FileForgeViewModel,
    modifier: Modifier = Modifier
) {
    val queue by viewModel.convertQueue.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val sourceFormat by viewModel.sourceFormat.collectAsState()
    val targetFormat by viewModel.targetFormat.collectAsState()
    val ocrEnabled by viewModel.ocrEnabled.collectAsState()
    val rasterDpi by viewModel.rasterDpi.collectAsState()
    val keepAnnotations by viewModel.keepAnnotations.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addMultiplePickedFiles(uris, com.example.ui.viewmodel.ScreenTab.CONVERT)
    }

    // Photo Picker launcher using ActivityResultContracts.PickMultipleVisualMedia
    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        viewModel.addMultiplePickedFiles(uris, com.example.ui.viewmodel.ScreenTab.CONVERT)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Header Text Group
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Convert Files",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = "Fast, private cross-format transformation right on your device sandbox.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            EngineStatusBanner(
                title = "100% Offline Engine",
                subtitle = "Zero server data transfer"
            )
        }

        // Dual-Engine Selector Card
        item {
            DualEngineSelectorCard(
                sourceFormat = sourceFormat,
                targetFormat = targetFormat,
                onSwap = { viewModel.swapFormats() },
                onSelectSource = { viewModel.setPresetRecipe(it, targetFormat) },
                onSelectTarget = { viewModel.setPresetRecipe(sourceFormat, it) },
                onRecipeSelected = { src, tgt -> viewModel.setPresetRecipe(src, tgt) }
            )
        }

        // Ingestion Drop Surface
        item {
            ConvertDropzoneCard(
                onBrowse = { filePickerLauncher.launch(arrayOf("*/*")) },
                onPickPhotos = { visualMediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            )
        }

        // Active Queue
        if (queue.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Queue (${queue.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "4 Threads Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontFamily = MonoFont
                        )
                    }
                }
            }

            items(queue, key = { it.id }) { item ->
                ConvertQueueItemCard(
                    item = item,
                    onRemove = { viewModel.removeConvertItem(item.id) }
                )
            }
        }

        // Engine Preferences Card
        item {
            EnginePreferencesCard(
                ocrEnabled = ocrEnabled,
                onToggleOcr = { viewModel.toggleOcr() },
                rasterDpi = rasterDpi,
                onSelectRasterDpi = { viewModel.setRasterDpi(it) },
                keepAnnotations = keepAnnotations,
                onToggleAnnotations = { viewModel.toggleAnnotations() }
            )
        }

        // Security Box
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Conversions execute entirely within an on-device isolated sandbox. Zero outbound sockets; temporary memory allocations are wiped immediately upon processing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Real-time asynchronous processing queue status
        if (isProcessing || queue.any { it.status == QueueStatus.PROCESSING || it.status == QueueStatus.COMPLETED }) {
            item {
                AsyncProcessingQueueCard(
                    isProcessing = isProcessing,
                    progress = progress,
                    progressMessage = progressMessage,
                    queuedItems = queue
                )
            }
        }

        // Primary Convert CTA
        if (queue.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.executeConvertBatch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("convert_all_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Convert ${queue.size} Files Simultaneously (Offline)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { viewModel.clearConvertQueue() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Clear Queue",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DualEngineSelectorCard(
    sourceFormat: String,
    targetFormat: String,
    onSwap: () -> Unit,
    onSelectSource: (String) -> Unit,
    onSelectTarget: (String) -> Unit,
    onRecipeSelected: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Source and Target Boxes with central swap button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Box
                FormatSelectorBox(
                    label = "FROM FORMAT",
                    selectedFormat = sourceFormat,
                    onSelect = onSelectSource,
                    modifier = Modifier.weight(1f)
                )

                // Central Swap Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                        .clickable { onSwap() }
                        .testTag("swap_formats_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Swap Formats",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Target Box
                FormatSelectorBox(
                    label = "TO FORMAT",
                    selectedFormat = targetFormat,
                    onSelect = onSelectTarget,
                    modifier = Modifier.weight(1f)
                )
            }

            // Common Recipes Carousel
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Popular Recipes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                val recipes = listOf(
                    Pair("PDF", "DOCX"),
                    Pair("PNG", "PDF"),
                    Pair("DOCX", "PDF"),
                    Pair("XLSX", "PDF"),
                    Pair("PDF", "JPG"),
                    Pair("JPG", "WEBP")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(recipes) { recipe ->
                        val isCurrent = sourceFormat == recipe.first && targetFormat == recipe.second
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.clickable { onRecipeSelected(recipe.first, recipe.second) }
                        ) {
                            Text(
                                text = "${recipe.first} → ${recipe.second}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoFont,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormatSelectorBox(
    label: String,
    selectedFormat: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val formats = listOf("PDF", "DOCX", "PNG", "JPG", "WEBP", "PPTX", "XLSX")

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clickable { expanded = true }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = MonoFont
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFormat,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = MonoFont
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            formats.forEach { fmt ->
                DropdownMenuItem(
                    text = { Text(fmt, fontFamily = MonoFont) },
                    onClick = {
                        onSelect(fmt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ConvertDropzoneCard(
    onBrowse: () -> Unit,
    onPickPhotos: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBrowse() }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Tap to select or drop files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Hardware accelerated local ingestion (Max 500MB)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable { onPickPhotos() }
                        .testTag("convert_pick_photos_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Pick Photos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Text(
                        text = "Local Engine Standby",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = MonoFont,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ConvertQueueItemCard(
    item: FileItem,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.format,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFont,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = EngineUtils.formatFileSize(item.originalSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        text = "Output: .${item.targetFormat.lowercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                }

                if (item.featureTag.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.featureTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EnginePreferencesCard(
    ocrEnabled: Boolean,
    onToggleOcr: () -> Unit,
    rasterDpi: String,
    onSelectRasterDpi: (String) -> Unit,
    keepAnnotations: Boolean,
    onToggleAnnotations: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Engine Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Hermetic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = MonoFont,
                        fontSize = 10.sp
                    )
                }
            }

            // OCR Text Recognition Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("OCR Text Recognition", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Local lightweight Tesseract engine", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(
                    checked = ocrEnabled,
                    onCheckedChange = { onToggleOcr() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Embedded Raster Resolution
            var dpiExpanded by remember { mutableStateOf(false) }
            val dpiList = listOf("300 DPI", "150 DPI", "72 DPI")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Embedded Raster Resolution", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Rasterization scale for embedded assets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }

                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.clickable { dpiExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(rasterDpi, style = MaterialTheme.typography.labelSmall, fontFamily = MonoFont, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(expanded = dpiExpanded, onDismissRequest = { dpiExpanded = false }) {
                        dpiList.forEach { dpi ->
                            DropdownMenuItem(
                                text = { Text(dpi, fontFamily = MonoFont) },
                                onClick = {
                                    onSelectRasterDpi(dpi)
                                    dpiExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Keep Annotations & Comments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep Annotations & Comments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("Preserve highlights and marginalia in output", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                Switch(
                    checked = keepAnnotations,
                    onCheckedChange = { onToggleAnnotations() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Local Target Directory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "/Files/FileForge/Converted/",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }
        }
    }
}
