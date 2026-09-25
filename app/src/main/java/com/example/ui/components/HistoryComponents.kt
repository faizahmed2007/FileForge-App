package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.HistoryItem
import com.example.engine.EngineUtils
import com.example.engine.FileOpener
import com.example.ui.theme.MonoFont
import com.example.ui.viewmodel.FileItem
import com.example.ui.viewmodel.QueueStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Real-time asynchronous processing queue status card with visual progress bar and spinner.
 */
@Composable
fun AsyncProcessingQueueCard(
    isProcessing: Boolean,
    progress: Float,
    progressMessage: String,
    queuedItems: List<FileItem>,
    modifier: Modifier = Modifier
) {
    if (!isProcessing && queuedItems.none { it.status == QueueStatus.PROCESSING }) {
        return
    }

    val activeCount = queuedItems.count { it.status == QueueStatus.PROCESSING }
    val completedCount = queuedItems.count { it.status == QueueStatus.COMPLETED }
    val totalCount = queuedItems.size.coerceAtLeast(1)
    val pct = (progress * 100).toInt().coerceIn(0, 100)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("async_processing_queue_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with circular spinner and real-time status
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
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isProcessing) "Async Processing Queue" else "Queue Processing Complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isProcessing) "$activeCount processing concurrently • $completedCount of $totalCount done"
                            else "All $completedCount files processed and saved to Room DB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Real-time Percentage Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$pct%",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Visual Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("async_queue_progress_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = progressMessage.ifBlank { "Processing asynchronous queue on device..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$completedCount / $totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            // Real-time Queue Item Chips
            if (queuedItems.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(queuedItems, key = { it.id }) { item ->
                        QueueItemStatusChip(item = item)
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItemStatusChip(item: FileItem) {
    val (statusColor, statusIcon, label) = when (item.status) {
        QueueStatus.PROCESSING -> Triple(MaterialTheme.colorScheme.primary, null, "Processing")
        QueueStatus.COMPLETED -> Triple(MaterialTheme.colorScheme.secondary, Icons.Default.Check, "Done")
        QueueStatus.FAILED -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Close, "Failed")
        QueueStatus.IDLE -> Triple(MaterialTheme.colorScheme.onSurfaceVariant, null, "Queued")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (item.status == QueueStatus.PROCESSING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = statusColor
                )
            } else if (statusIcon != null) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                fontSize = 11.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Storage dashboard that calculates and displays the total space saved by compressing files,
 * using local metadata from the Room database.
 */
@Composable
fun StorageSavingsDashboardCard(
    totalSpaceSaved: Long,
    historyCount: Int,
    totalOriginalBytes: Long,
    totalOutputBytes: Long,
    onViewHistory: () -> Unit = {}
) {
    val totalSavedPositive = totalSpaceSaved.coerceAtLeast(0L)
    val percentageSaved = if (totalOriginalBytes > 0) {
        ((totalSavedPositive.toDouble() / totalOriginalBytes.toDouble()) * 100).toInt().coerceIn(0, 99)
    } else if (totalSavedPositive > 0) {
        72 // estimated percentage
    } else {
        0
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("storage_savings_dashboard")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF10B981),
                                        Color(0xFF059669)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage Dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Storage Savings Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Room SQLite local telemetry",
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
                        text = "$historyCount Jobs in DB",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Primary Stat: Total Space Saved
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total Space Reclaimed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = EngineUtils.formatFileSize(totalSavedPositive),
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = (-0.03).sp
                    )
                }

                if (percentageSaved > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "-$percentageSaved% Saved",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Comparison Metrics Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Original Size
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Original Input",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (totalOriginalBytes > 0) EngineUtils.formatFileSize(totalOriginalBytes) else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    // Output Size
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Compressed Size",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (totalOutputBytes > 0) EngineUtils.formatFileSize(totalOutputBytes) else "--",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    // Efficiency
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Compression Ratio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (percentageSaved > 0) "${percentageSaved}:100" else "1:1",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Search bar at the top of history view to filter by name or file type.
 */
@Composable
fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    totalMatches: Int,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_search_bar"),
        placeholder = {
            Text(
                text = "Search by file name or format (e.g. .pdf, .png, photo)...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (totalMatches > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$totalMatches",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFont,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    )
}

/**
 * Batch action toolbar for multiple selection, "Select All", "Delete Selected", "Batch Rename", "Export".
 */
@Composable
fun HistoryBatchToolbar(
    totalItemsCount: Int,
    selectedCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBatchRename: () -> Unit,
    onExportSelected: () -> Unit,
    onExitSelectionMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth().testTag("batch_selection_toolbar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Status badge and Done/Exit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (selectedCount > 0) "$selectedCount of $totalItemsCount Selected" else "Batch Selection Mode",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                TextButton(
                    onClick = onExitSelectionMode,
                    modifier = Modifier.testTag("exit_selection_mode_button")
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Bottom Row: Select all toggle & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Checkbox(
                        checked = allSelected && totalItemsCount > 0,
                        onCheckedChange = { checked ->
                            if (checked) onSelectAll() else onDeselectAll()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(20.dp).testTag("select_all_checkbox")
                    )
                    TextButton(
                        onClick = {
                            if (allSelected) onDeselectAll() else onSelectAll()
                        },
                        modifier = Modifier.testTag("select_all_button")
                    ) {
                        Text(
                            text = if (allSelected) "Deselect All" else "Select All ($totalItemsCount)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedCount > 0) {
                        // Export Selected
                        IconButton(
                            onClick = onExportSelected,
                            modifier = Modifier.size(34.dp).testTag("export_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Export Selected",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Batch Rename
                        IconButton(
                            onClick = onBatchRename,
                            modifier = Modifier.size(34.dp).testTag("rename_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DriveFileRenameOutline,
                                contentDescription = "Rename Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Delete Selected Button
                        Button(
                            onClick = onDeleteSelected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("delete_selected_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Delete ($selectedCount)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Enhanced HistoryRowCard with:
 * - Coil AsyncImage thumbnail for images
 * - File format badge for PDF/Word/Excel/PowerPoint
 * - Selection checkbox for batch delete / rename / export
 * - Direct "Open File" button to launch system viewers for .pdf/.xlsx/.pptx/.doc
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EnhancedHistoryRowCard(
    item: HistoryItem,
    isSelected: Boolean,
    selectionMode: Boolean = false,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onOpenFile: () -> Unit,
    onDelete: () -> Unit,
    onSaveToGallery: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember(item.outputPath) { File(item.outputPath) }
    val isImage = remember(item.outputPath, item.fileName) {
        FileOpener.isImage(item.outputPath) || FileOpener.isImage(item.fileName)
    }
    val isPdf = remember(item.outputPath, item.fileName) {
        FileOpener.isPdf(item.outputPath) || FileOpener.isPdf(item.fileName)
    }
    val isOffice = remember(item.outputPath, item.fileName) {
        FileOpener.isOfficeDoc(item.outputPath) || FileOpener.isOfficeDoc(item.fileName)
    }

    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(item.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else null,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelect() else onClick()
                },
                onLongClick = {
                    onLongClick()
                }
            )
            .testTag("history_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox for batch selection
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(24.dp).testTag("checkbox_${item.id}")
            )

            // Coil Thumbnail or Format Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onOpenFile() },
                contentAlignment = Alignment.Center
            ) {
                if (isImage && file.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(file)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Thumbnail for ${item.fileName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .testTag("thumbnail_${item.id}")
                    )
                } else if (isPdf) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Document",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isOffice) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Office Document",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.targetFormat.take(4).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "File",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = item.targetFormat.take(4).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // File Information & Room Metadata
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

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // File Opener button (opens .pdf, .xlsx, .pptx, .doc, images, etc.)
                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(28.dp).testTag("open_file_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Open File with System App",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Save to Gallery
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

                // Share
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(28.dp).testTag("share_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp).testTag("delete_history_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete from Room DB",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Batch rename dialog for appending custom prefixes or timestamps.
 */
@Composable
fun BatchRenameDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (prefix: String, appendTimestamp: Boolean) -> Unit
) {
    var prefix by remember { mutableStateOf("optimized_") }
    var appendTimestamp by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DriveFileRenameOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Batch Rename ($selectedCount Files)")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Apply a consistent prefix or timestamp to all $selectedCount selected records in the Room database and local storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Filename Prefix") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Append Timestamp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "e.g. 20260924_1530_",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appendTimestamp,
                        onCheckedChange = { appendTimestamp = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(prefix, appendTimestamp) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Rename $selectedCount Files")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * File preview & opener dialog for inspecting and opening all file types (.pdf/.xlsx/.pptx/.doc/images).
 */
@Composable
fun FileViewerOpenerDialog(
    item: HistoryItem,
    onDismiss: () -> Unit,
    onExportSaf: () -> Unit,
    onSaveToGallery: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(item.outputPath) { File(item.outputPath) }
    val isImage = remember(item.outputPath, item.fileName) {
        FileOpener.isImage(item.outputPath) || FileOpener.isImage(item.fileName)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with file name and close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isImage) Icons.Default.Image else Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "File Viewer & Opener",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Preview Content Area
                if (isImage && file.exists()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(file)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (FileOpener.isPdf(item.fileName)) Icons.Default.PictureAsPdf
                                else if (FileOpener.isOfficeDoc(item.fileName)) Icons.Default.Description
                                else Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "MIME: ${FileOpener.getMimeType(file)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonoFont,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // File Details Table
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DetailItemRow("Format", item.targetFormat.uppercase())
                        DetailItemRow("Output Size", EngineUtils.formatFileSize(item.outputSizeBytes))
                        DetailItemRow("Saved", "${item.savedPercentage}% (${EngineUtils.formatFileSize((item.originalSizeBytes - item.outputSizeBytes).coerceAtLeast(0L))})")
                        DetailItemRow("Path", file.absolutePath)
                        DetailItemRow("Status", if (file.exists()) "Stored Locally On Device" else "Deleted from storage")
                    }
                }

                // Opener and Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            FileOpener.openFileWithSystemApp(context, file)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_with_system_app_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open with System App (.pdf/.xlsx/.pptx/etc)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportSaf,
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export SAF", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { FileOpener.shareFile(context, file) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = MonoFont,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Confirmation dialog for batch file deletion to prevent accidental data loss.
 * Displays preview of files being deleted and an option to also delete files from storage.
 */
@Composable
fun BatchDeleteConfirmationDialog(
    selectedItems: List<HistoryItem>,
    onDismiss: () -> Unit,
    onConfirmDelete: (deletePhysicalFiles: Boolean) -> Unit
) {
    var deletePhysicalFiles by remember { mutableStateOf(false) }
    val count = selectedItems.size

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Delete $count Selected Files?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You are about to delete $count file records from your local Room database. This operation cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // List preview of files
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedItems.take(4).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.fileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = MonoFont,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (selectedItems.size > 4) {
                            Text(
                                text = "+ ${selectedItems.size - 4} more files...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }

                // Checkbox to also delete physical files from storage
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().clickable { deletePhysicalFiles = !deletePhysicalFiles }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = deletePhysicalFiles,
                            onCheckedChange = { deletePhysicalFiles = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                        )
                        Column {
                            Text(
                                text = "Also delete local files from storage",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Permanently removes output files from device disk",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDelete(deletePhysicalFiles) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_batch_delete_button")
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete $count Files")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_batch_delete_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
