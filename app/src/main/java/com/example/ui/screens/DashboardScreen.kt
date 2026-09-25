package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.compose.material.icons.filled.Checklist
import com.example.data.model.HistoryItem
import com.example.engine.EngineUtils
import com.example.engine.FileOpener
import com.example.ui.components.AsyncProcessingQueueCard
import com.example.ui.components.BatchDeleteConfirmationDialog
import com.example.ui.components.BatchRenameDialog
import com.example.ui.components.EnhancedHistoryRowCard
import com.example.ui.components.EngineStatusBanner
import com.example.ui.components.FileViewerOpenerDialog
import com.example.ui.components.HistoryBatchToolbar
import com.example.ui.components.HistorySearchBar
import com.example.ui.components.StorageSavingsDashboardCard
import com.example.ui.theme.MonoFont
import com.example.ui.viewmodel.FileForgeViewModel
import com.example.ui.viewmodel.FileItem
import com.example.ui.viewmodel.QueueStatus
import com.example.ui.viewmodel.ScreenTab
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: FileForgeViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList by viewModel.filteredHistoryList.collectAsState()
    val historyFilter by viewModel.historyFilter.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val selectedIds by viewModel.selectedHistoryIds.collectAsState()
    val totalSpaceSaved by viewModel.totalSpaceSaved.collectAsState()
    val totalCompressOriginalBytes by viewModel.totalCompressOriginalBytes.collectAsState()
    val totalCompressOutputBytes by viewModel.totalCompressOutputBytes.collectAsState()
    val historyCount by viewModel.historyCount.collectAsState()
    val compressQueue by viewModel.compressQueue.collectAsState()
    val convertQueue by viewModel.convertQueue.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val progressMessage by viewModel.progressMessage.collectAsState()

    var selectedHistoryMetadata by remember { mutableStateOf<HistoryItem?>(null) }
    var fileViewerItem by remember { mutableStateOf<HistoryItem?>(null) }
    var isBatchSelectionMode by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedConfirmDialog by remember { mutableStateOf(false) }
    var showBatchRenameDialog by remember { mutableStateOf(false) }
    var pendingExportFile by remember { mutableStateOf<File?>(null) }

    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null && pendingExportFile != null) {
            val success = FileOpener.exportFileToSafUri(context, pendingExportFile!!, uri)
            if (success) {
                android.widget.Toast.makeText(context, "Exported successfully to device storage!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Export failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        pendingExportFile = null
    }

    // Multi-selection Image Picker (up to 50 images at once)
    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addMultiplePickedFiles(uris, ScreenTab.COMPRESS)
        }
    }

    // Multi-selection Document / File Picker
    val multiDocumentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addMultiplePickedFiles(uris, ScreenTab.COMPRESS)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Hero Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Your files. Your device. Your privacy.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = "Compress, resize, and convert multiple files simultaneously with 100% on-device concurrency.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            EngineStatusBanner(
                title = "100% Offline Processing Engine",
                subtitle = "Local Room database metadata • Zero network transfer"
            )
        }

        // Multi-Selection File Picker & Simultaneous Batch Queue Studio
        item {
            MultiSelectionBatchStudioCard(
                queuedCompressCount = compressQueue.size,
                queuedConvertCount = convertQueue.size,
                isProcessing = isProcessing,
                progress = progress,
                progressMessage = progressMessage,
                onPickMultipleImages = {
                    multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onPickMultipleFiles = {
                    multiDocumentPicker.launch(arrayOf("*/*"))
                },
                onAddSampleBatch = {
                    viewModel.addSampleBatchImages(ScreenTab.COMPRESS)
                },
                onCompressBatchSimultaneously = {
                    viewModel.executeCompressBatch()
                },
                onConvertBatchSimultaneously = { targetFormat ->
                    viewModel.executeConvertBatch(targetFormatOverride = targetFormat)
                },
                onClearQueue = {
                    viewModel.clearCompressQueue()
                    viewModel.clearConvertQueue()
                },
                compressItems = compressQueue,
                onViewFullQueue = { viewModel.selectTab(ScreenTab.COMPRESS) }
            )
        }

        // Real-time Visual Progress Bar & Spinner for Asynchronous Queue
        item {
            AsyncProcessingQueueCard(
                isProcessing = isProcessing,
                progress = progress,
                progressMessage = progressMessage,
                queuedItems = compressQueue + convertQueue
            )
        }

        // Storage Savings Dashboard from Room Database
        item {
            StorageSavingsDashboardCard(
                totalSpaceSaved = totalSpaceSaved,
                historyCount = historyCount,
                totalOriginalBytes = totalCompressOriginalBytes,
                totalOutputBytes = totalCompressOutputBytes
            )
        }

        // Core Utility Navigation Cards
        item {
            Text(
                text = "File Utilities",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UtilityFeatureCard(
                        title = "Compress",
                        description = "Simultaneous multi-image compression with adaptive fidelity",
                        icon = Icons.Default.FolderZip,
                        badge = "Parallel Queue",
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(ScreenTab.COMPRESS) }
                    )

                    UtilityFeatureCard(
                        title = "Convert",
                        description = "Simultaneous format conversion (PNG, JPG, WebP, PDF)",
                        icon = Icons.Default.SyncAlt,
                        badge = "Multi-thread",
                        iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(ScreenTab.CONVERT) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UtilityFeatureCard(
                        title = "Resize Image",
                        description = "Precise dimensions, percentage scaling & aspect ratio locks",
                        icon = Icons.Default.AspectRatio,
                        badge = "Offline",
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(ScreenTab.TOOLS) }
                    )

                    UtilityFeatureCard(
                        title = "PDF Toolkit",
                        description = "Merge, Split, Rotate, and Stamp Watermarks",
                        icon = Icons.Default.PictureAsPdf,
                        badge = "Vector",
                        iconTint = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.selectTab(ScreenTab.TOOLS) }
                    )
                }
            }
        }

        // Privacy Section Showcase
        item {
            PrivacyShowcaseCard()
        }

        // Recent Processing Activity & History (Stored in Room Database)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recent Processing Activity",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Room DB ($historyCount)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoFont,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (historyList.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    val files = historyList.map { File(it.outputPath) }.filter { it.exists() }
                                    if (files.isNotEmpty()) {
                                        viewModel.saveFilesToGallery(files)
                                    } else {
                                        android.widget.Toast.makeText(context, "No local files found to save", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("save_all_history_to_gallery")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Save All to Gallery",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    isBatchSelectionMode = !isBatchSelectionMode
                                    if (!isBatchSelectionMode) {
                                        viewModel.clearHistorySelection()
                                    }
                                },
                                modifier = Modifier.testTag("toggle_batch_selection_mode")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isBatchSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (isBatchSelectionMode) "Done" else "Batch Select",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isBatchSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            TextButton(
                                onClick = { showClearConfirmDialog = true },
                                modifier = Modifier.testTag("clear_history_button")
                            ) {
                                Text(
                                    text = "Clear All",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Search Bar at the Top of History View (filter by name or file type)
                HistorySearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setHistorySearchQuery(it) },
                    totalMatches = historyList.size
                )

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = historyFilter == "ALL",
                        onClick = { viewModel.setHistoryFilter("ALL") },
                        label = { Text("All Jobs") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = historyFilter == "COMPRESS",
                        onClick = { viewModel.setHistoryFilter("COMPRESS") },
                        label = { Text("Compressed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = historyFilter == "CONVERT",
                        onClick = { viewModel.setHistoryFilter("CONVERT") },
                        label = { Text("Converted") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                // Batch Selection Toolbar ("Select All", "Delete Selected", "Batch Rename", "Export")
                if (historyList.isNotEmpty() && (isBatchSelectionMode || selectedIds.isNotEmpty())) {
                    HistoryBatchToolbar(
                        totalItemsCount = historyList.size,
                        selectedCount = selectedIds.size,
                        allSelected = selectedIds.size == historyList.size && historyList.isNotEmpty(),
                        onSelectAll = { viewModel.selectAllHistory(historyList.map { it.id }) },
                        onDeselectAll = { viewModel.clearHistorySelection() },
                        onDeleteSelected = { showDeleteSelectedConfirmDialog = true },
                        onBatchRename = { showBatchRenameDialog = true },
                        onExportSelected = {
                            val selectedItems = historyList.filter { it.id in selectedIds }
                            val firstFile = selectedItems.map { File(it.outputPath) }.firstOrNull { it.exists() }
                            if (firstFile != null) {
                                pendingExportFile = firstFile
                                safExportLauncher.launch(firstFile.name)
                            } else {
                                android.widget.Toast.makeText(context, "Selected files not found on device", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExitSelectionMode = {
                            isBatchSelectionMode = false
                            viewModel.clearHistorySelection()
                        }
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No records matching \"$searchQuery\""
                            else if (historyFilter == "ALL") "No Processing History in Room Database"
                            else "No matching jobs for this filter",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different file name or extension (.png, .pdf, .docx)."
                            else "Use the Multi-Selection Picker above to compress or convert multiple images simultaneously. Metadata will be persisted here in SQLite.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(historyList, key = { it.id }) { item ->
                EnhancedHistoryRowCard(
                    item = item,
                    isSelected = selectedIds.contains(item.id),
                    selectionMode = isBatchSelectionMode || selectedIds.isNotEmpty(),
                    onToggleSelect = {
                        isBatchSelectionMode = true
                        viewModel.toggleHistorySelection(item.id)
                    },
                    onClick = { fileViewerItem = item },
                    onLongClick = {
                        isBatchSelectionMode = true
                        viewModel.toggleHistorySelection(item.id)
                    },
                    onOpenFile = {
                        val file = File(item.outputPath)
                        val opened = FileOpener.openFileWithSystemApp(context, file)
                        if (!opened) {
                            fileViewerItem = item
                        }
                    },
                    onDelete = { viewModel.deleteHistoryItem(item.id) },
                    onSaveToGallery = {
                        val file = File(item.outputPath)
                        if (file.exists()) {
                            viewModel.saveProcessedFileToGallery(file)
                        } else {
                            android.widget.Toast.makeText(context, "Output file does not exist locally: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShare = {
                        val file = File(item.outputPath)
                        if (file.exists()) {
                            FileOpener.shareFile(context, file)
                        } else {
                            android.widget.Toast.makeText(context, "Cannot share file: ${file.name}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Detailed File Viewer & Opener Dialog (Supports all types: .pdf, .xlsx, .pptx, .doc, images)
    fileViewerItem?.let { item ->
        FileViewerOpenerDialog(
            item = item,
            onDismiss = { fileViewerItem = null },
            onExportSaf = {
                val file = File(item.outputPath)
                if (file.exists()) {
                    pendingExportFile = file
                    safExportLauncher.launch(file.name)
                } else {
                    android.widget.Toast.makeText(context, "File does not exist locally", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onSaveToGallery = {
                val file = File(item.outputPath)
                if (file.exists()) {
                    viewModel.saveProcessedFileToGallery(file)
                } else {
                    android.widget.Toast.makeText(context, "Output file does not exist locally", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Batch Rename Dialog
    if (showBatchRenameDialog && selectedIds.isNotEmpty()) {
        BatchRenameDialog(
            selectedCount = selectedIds.size,
            onDismiss = { showBatchRenameDialog = false },
            onConfirm = { prefix, appendTimestamp ->
                viewModel.batchRenameHistory(selectedIds, prefix, appendTimestamp)
                showBatchRenameDialog = false
            }
        )
    }

    // Confirmation dialog for deleting selected history records from Room database to prevent accidental data loss
    if (showDeleteSelectedConfirmDialog && selectedIds.isNotEmpty()) {
        val selectedItems = historyList.filter { it.id in selectedIds }
        BatchDeleteConfirmationDialog(
            selectedItems = selectedItems,
            onDismiss = { showDeleteSelectedConfirmDialog = false },
            onConfirmDelete = { deletePhysicalFiles ->
                viewModel.deleteSelectedHistory(deletePhysicalFiles)
                showDeleteSelectedConfirmDialog = false
                isBatchSelectionMode = false
            }
        )
    }

    // Detailed Room Metadata Dialog
    selectedHistoryMetadata?.let { item ->
        RoomMetadataDetailDialog(
            item = item,
            onDismiss = { selectedHistoryMetadata = null },
            onSaveToGallery = {
                val file = File(item.outputPath)
                if (file.exists()) {
                    viewModel.saveProcessedFileToGallery(file)
                } else {
                    android.widget.Toast.makeText(context, "Output file does not exist locally", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onDelete = {
                viewModel.deleteHistoryItem(item.id)
                selectedHistoryMetadata = null
            }
        )
    }

    // Confirmation dialog to clear all Room history
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Processing History?") },
            text = {
                Text("This will permanently remove all processing job records from your local Room database. Output files stored on your device will remain intact.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * High-impact Multi-Selection Batch Studio card with simultaneous queue execution.
 */
@Composable
fun MultiSelectionBatchStudioCard(
    queuedCompressCount: Int,
    queuedConvertCount: Int,
    isProcessing: Boolean,
    progress: Float,
    progressMessage: String,
    onPickMultipleImages: () -> Unit,
    onPickMultipleFiles: () -> Unit,
    onAddSampleBatch: () -> Unit,
    onCompressBatchSimultaneously: () -> Unit,
    onConvertBatchSimultaneously: (String) -> Unit,
    onClearQueue: () -> Unit,
    compressItems: List<FileItem>,
    onViewFullQueue: () -> Unit
) {
    var showConvertFormatMenu by remember { mutableStateOf(false) }
    val totalQueued = queuedCompressCount + queuedConvertCount

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("multi_selection_studio_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Simultaneous Batch Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Multi-file concurrent async pipeline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Concurrent 4x",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Description
            Text(
                text = "Select multiple images or documents simultaneously to compress or convert in parallel across device CPU cores without network delays.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            // Picker Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickMultipleImages,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("pick_multiple_images_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Pick Multiple Images",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Select Images", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onPickMultipleFiles,
                    modifier = Modifier.weight(1f).height(44.dp).testTag("pick_multiple_files_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Pick Multiple Documents",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Select Files", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Quick Batch Sample Load button
            OutlinedButton(
                onClick = onAddSampleBatch,
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("load_sample_batch_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Load 3 Sample Images (4K, HD, WebP) for Batch Test",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Live Progress indicator when processing
            if (isProcessing) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progressMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }

            // Active Queued Items Preview & Execution CTA
            AnimatedVisibility(visible = totalQueued > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                text = "$totalQueued Files in Processing Queue",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = onClearQueue,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }

                    // Mini preview horizontal chip list of queued files
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(compressItems) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.format,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = MonoFont,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    if (item.status == QueueStatus.COMPLETED) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Done",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Simultaneous Execution Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCompressBatchSimultaneously,
                            enabled = !isProcessing && totalQueued > 0,
                            modifier = Modifier.weight(1f).height(46.dp).testTag("simultaneous_compress_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Compress All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { showConvertFormatMenu = true },
                                enabled = !isProcessing && totalQueued > 0,
                                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("simultaneous_convert_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Convert All", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            DropdownMenu(
                                expanded = showConvertFormatMenu,
                                onDismissRequest = { showConvertFormatMenu = false }
                            ) {
                                listOf("WEBP", "PNG", "JPEG", "PDF").forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text("Simultaneous Convert to $format") },
                                        onClick = {
                                            showConvertFormatMenu = false
                                            onConvertBatchSimultaneously(format)
                                        }
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

/**
 * Statistics banner demonstrating real-time Room database storage.
 */
@Composable
fun RoomDatabaseStatsCard(
    historyCount: Int,
    totalSpaceSaved: Long
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Local Room Database",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$historyCount completed jobs indexed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = EngineUtils.formatFileSize(totalSpaceSaved),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Total Space Saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun UtilityFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .clickable { onClick() }
            .testTag("tool_card_${title.lowercase().replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PrivacyShowcaseCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Private by design",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "All processing happens locally on your device. Your files and processing metadata are kept in SQLite Room on device and never uploaded to any remote server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            val guarantees = listOf(
                "Zero telemetry, tracking, or network uploads",
                "Hermetic on-device memory sandboxing",
                "Local SQLite Room database persistence",
                "100% functional without internet connectivity"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                guarantees.forEach { g ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = g,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich History row card showing comprehensive metadata from the Room database.
 */
@Composable
fun HistoryRowCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("history_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.targetFormat.take(4).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.fileName,
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
                        text = item.operation,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

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
                    Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Text(
                        text = EngineUtils.formatFileSize(item.outputSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                    if (item.savedPercentage > 0) {
                        Text(
                            text = "(-${item.savedPercentage}%)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onSaveToGallery,
                    modifier = Modifier.size(28.dp).testTag("save_gallery_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Save to Media Gallery",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(28.dp).testTag("share_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Metadata Dialog for inspecting the full database record of a processed file.
 */
@Composable
fun RoomMetadataDetailDialog(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Job Metadata (Room DB)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetadataDetailRow(label = "File Name", value = item.fileName)
                MetadataDetailRow(label = "Operation", value = item.operation)
                MetadataDetailRow(label = "Formats", value = "${item.originalFormat} → ${item.targetFormat}")
                MetadataDetailRow(label = "Original Size", value = "${EngineUtils.formatFileSize(item.originalSizeBytes)} (${item.originalSizeBytes} bytes)")
                MetadataDetailRow(label = "Output Size", value = "${EngineUtils.formatFileSize(item.outputSizeBytes)} (${item.outputSizeBytes} bytes)")
                MetadataDetailRow(label = "Space Saved", value = "${EngineUtils.formatFileSize(item.spaceSavedBytes)} (${item.savedPercentage}%)")
                MetadataDetailRow(label = "Recorded At", value = dateStr)
                if (item.details.isNotBlank()) {
                    MetadataDetailRow(label = "Details", value = item.details)
                }
                MetadataDetailRow(label = "Status", value = item.status)
                MetadataDetailRow(label = "Local Path", value = item.outputPath, isPath = true)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveToGallery,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Save to Gallery")
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Record")
            }
        }
    )
}

@Composable
fun MetadataDetailRow(label: String, value: String, isPath: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (isPath) MonoFont else androidx.compose.ui.text.font.FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (isPath) 10.sp else 12.sp,
            fontWeight = if (isPath) FontWeight.Normal else FontWeight.Medium
        )
    }
}
