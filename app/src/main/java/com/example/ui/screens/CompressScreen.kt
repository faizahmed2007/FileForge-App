package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TrendingDown
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.engine.EngineUtils
import com.example.ui.components.AsyncProcessingQueueCard
import com.example.ui.components.EngineStatusBanner
import com.example.ui.theme.MonoFont
import com.example.ui.viewmodel.FileForgeViewModel
import com.example.ui.viewmodel.FileItem
import com.example.ui.viewmodel.QueueStatus
import com.example.ui.viewmodel.ScreenTab

@Composable
fun CompressScreen(
    viewModel: FileForgeViewModel,
    modifier: Modifier = Modifier
) {
    val queue by viewModel.compressQueue.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()
    val compressionStrength by viewModel.compressionStrength.collectAsState()
    val targetCap by viewModel.targetCap.collectAsState()
    val visualFidelity by viewModel.visualFidelity.collectAsState()
    val dpiResampling by viewModel.dpiResampling.collectAsState()
    val pdfSubsetting by viewModel.pdfSubsetting.collectAsState()
    val stripMetadata by viewModel.stripMetadata.collectAsState()
    val isAdvancedExpanded by viewModel.advancedExpanded.collectAsState()

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addMultiplePickedFiles(uris, ScreenTab.COMPRESS)
    }

    // Photo Picker launcher using ActivityResultContracts.PickMultipleVisualMedia
    val visualMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        viewModel.addMultiplePickedFiles(uris, ScreenTab.COMPRESS)
    }

    val totalOriginal = queue.sumOf { it.originalSizeBytes }
    val totalEstOutput = queue.sumOf { it.estimatedOutputSizeBytes }
    val totalSaved = (totalOriginal - totalEstOutput).coerceAtLeast(0L)
    val savedPercent = if (totalOriginal > 0) ((totalSaved.toDouble() / totalOriginal) * 100).toInt() else 0

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
                    text = "Compress Files",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = "Reduce file size locally without losing fidelity.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            // Local Engine Status Banner
            EngineStatusBanner(
                title = "On-Device Engine Active",
                subtitle = "No files uploaded to servers"
            )
        }

        item {
            // Dropzone & Quick Picker
            DropzoneCard(
                onBrowse = { filePickerLauncher.launch(arrayOf("*/*")) },
                onPickPhotos = { visualMediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onAddPdf = { viewModel.addSamplePdf() },
                onAddImages = { viewModel.addSampleImage() }
            )
        }

        // Active Selected Files Section
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
                        text = "Selected Files (${queue.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Batch Ready",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = MonoFont
                    )
                }
            }

            items(queue, key = { it.id }) { item ->
                CompressFileCard(
                    item = item,
                    onRemove = { viewModel.removeCompressItem(item.id) }
                )
            }
        }

        // Compression Settings & Presets Card
        item {
            CompressionProfileCard(
                strength = compressionStrength,
                onStrengthSelected = { viewModel.setCompressionStrength(it) },
                targetCap = targetCap,
                onTargetCapSelected = { viewModel.setTargetCap(it) },
                visualFidelity = visualFidelity,
                onFidelityChanged = { viewModel.setVisualFidelity(it) },
                dpiResampling = dpiResampling,
                onDpiChanged = { viewModel.setDpiResampling(it) },
                pdfSubsetting = pdfSubsetting,
                onTogglePdfSubsetting = { viewModel.togglePdfSubsetting() },
                stripMetadata = stripMetadata,
                onToggleStripMetadata = { viewModel.toggleStripMetadata() },
                isAdvancedExpanded = isAdvancedExpanded,
                onToggleAdvanced = { viewModel.toggleAdvancedExpanded() }
            )
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

        // Before & After Visual Summary Banner
        if (queue.isNotEmpty()) {
            item {
                EstimatedYieldBanner(
                    totalOriginal = totalOriginal,
                    totalEstimated = totalEstOutput,
                    totalSaved = totalSaved,
                    savedPercent = savedPercent
                )
            }

            // Action CTA Group
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.executeCompressBatch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("compress_all_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflineBolt,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Compress ${queue.size} Files Simultaneously (${EngineUtils.formatFileSize(totalSaved)} saved)",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { viewModel.clearCompressQueue() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("clear_compress_queue"),
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
fun DropzoneCard(
    onBrowse: () -> Unit,
    onPickPhotos: () -> Unit,
    onAddPdf: () -> Unit,
    onAddImages: () -> Unit
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Main clickable drop box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                    .clickable { onBrowse() }
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tap to browse or drop files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Supported formats: PDF, PNG, JPG, WEBP, DOCX",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Quick Action Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clickable { onPickPhotos() }
                        .testTag("pick_photos_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clickable { onAddPdf() }
                        .testTag("add_sample_pdf_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+ PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clickable { onAddImages() }
                        .testTag("add_sample_image_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "+ Sample",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CompressFileCard(
    item: FileItem,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.format in listOf("PNG", "JPG", "JPEG", "WEBP") && item.file.exists()) {
                        AsyncImage(
                            model = item.file,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f))
                                .padding(vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.format,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                fontFamily = MonoFont,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = item.format,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontFamily = MonoFont,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // File Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.subInfo.isNotEmpty()) {
                            Text(
                                text = item.subInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "Original: ${EngineUtils.formatFileSize(item.originalSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Compression Yield Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = EngineUtils.formatFileSize(item.estimatedOutputSizeBytes),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "est.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "-${item.estimatedSavingsPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CompressionProfileCard(
    strength: String,
    onStrengthSelected: (String) -> Unit,
    targetCap: String,
    onTargetCapSelected: (String) -> Unit,
    visualFidelity: Int,
    onFidelityChanged: (Int) -> Unit,
    dpiResampling: String,
    onDpiChanged: (String) -> Unit,
    pdfSubsetting: Boolean,
    onTogglePdfSubsetting: () -> Unit,
    stripMetadata: Boolean,
    onToggleStripMetadata: () -> Unit,
    isAdvancedExpanded: Boolean,
    onToggleAdvanced: () -> Unit
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
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Compression Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Smart Adaptive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Compression Strength Segmented Control
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Compression Strength",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                val options = listOf("Max", "High", "Balanced", "Smallest")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    options.forEach { option ->
                        val isSelected = option == strength
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh
                                    else Color.Transparent
                                )
                                .clickable { onStrengthSelected(option) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Target Cap per File
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Target Cap per File",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )

                val caps = listOf("100 KB", "500 KB", "1 MB", "2 MB", "Custom...")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(caps) { cap ->
                        val isSelected = cap == targetCap
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainer
                                )
                                .clickable { onTargetCapSelected(cap) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cap,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Advanced Parameters Accordion
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleAdvanced() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Advanced Parameters",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isAdvancedExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Slider: Visual Fidelity
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Visual Fidelity",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$visualFidelity%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = MonoFont,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Slider(
                                    value = visualFidelity.toFloat(),
                                    onValueChange = { onFidelityChanged(it.toInt()) },
                                    valueRange = 40f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Aggressive (40%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text("Lossless (100%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }

                            // DPI Resampling Select
                            var showDpiDropdown by remember { mutableStateOf(false) }
                            val dpiOptions = listOf("72 DPI (Low Screen)", "150 DPI (Screen/Web)", "300 DPI (Print Quality)")

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("DPI Resampling", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Balances text sharpness & size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier.clickable { showDpiDropdown = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = dpiResampling.substringBefore(" ("),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = MonoFont,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.sp
                                            )
                                            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showDpiDropdown,
                                        onDismissRequest = { showDpiDropdown = false }
                                    ) {
                                        dpiOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, fontSize = 12.sp) },
                                                onClick = {
                                                    onDpiChanged(opt)
                                                    showDpiDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // PDF Subsetting switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PDF Subsetting & Compression", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Removes unused embedded font glyphs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = pdfSubsetting,
                                    onCheckedChange = { onTogglePdfSubsetting() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Strip Metadata & EXIF switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Strip Metadata & EXIF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("Privacy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp, fontFamily = MonoFont)
                                        }
                                    }
                                    Text("Purges GPS, camera details & timestamps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                                Switch(
                                    checked = stripMetadata,
                                    onCheckedChange = { onToggleStripMetadata() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EstimatedYieldBanner(
    totalOriginal: Long,
    totalEstimated: Long,
    totalSaved: Long,
    savedPercent: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ESTIMATED YIELD",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.05.sp
                )
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Saved ${EngineUtils.formatFileSize(totalSaved)} ($savedPercent% reduction)",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Dual metric numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Original Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    Text(
                        text = EngineUtils.formatFileSize(totalOriginal),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Compressed Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                    Text(
                        text = EngineUtils.formatFileSize(totalEstimated),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Yield Progress Bar
            val ratio = if (totalOriginal > 0) (totalEstimated.toFloat() / totalOriginal.toFloat()).coerceIn(0.05f, 1f) else 0.2f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}
