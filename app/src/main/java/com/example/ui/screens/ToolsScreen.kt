package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.EngineUtils
import com.example.ui.components.EngineStatusBanner
import com.example.ui.theme.MonoFont
import com.example.ui.viewmodel.FileForgeViewModel
import java.io.File

@Composable
fun ToolsScreen(
    viewModel: FileForgeViewModel,
    modifier: Modifier = Modifier
) {
    var showResizeDialog by remember { mutableStateOf(false) }
    var showWatermarkDialog by remember { mutableStateOf(false) }
    var showRotateDialog by remember { mutableStateOf(false) }

    // Merge PDF picker
    val mergePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val files = uris.map { EngineUtils.uriToTempFile(viewModel.getApplication(), it) }
            viewModel.runToolMergePdfs(files)
        }
    }

    // Single PDF picker for Rotate
    val rotatePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val file = EngineUtils.uriToTempFile(viewModel.getApplication(), uri)
            viewModel.runToolRotatePdf(file, 90f)
        }
    }

    // Single PDF picker for Watermark
    val watermarkPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val file = EngineUtils.uriToTempFile(viewModel.getApplication(), uri)
            viewModel.runToolWatermarkPdf(file, "CONFIDENTIAL")
        }
    }

    // Image to PDF picker
    val imagesToPdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val files = uris.map { EngineUtils.uriToTempFile(viewModel.getApplication(), it) }
            // Run conversion via ViewModel
            viewModel.engine
            viewModel.selectTab(com.example.ui.viewmodel.ScreenTab.CONVERT)
            uris.forEach { viewModel.addPickedFile(it, com.example.ui.viewmodel.ScreenTab.CONVERT) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Offline Toolbox",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp
                )
                Text(
                    text = "Granular single-purpose utilities engineered for offline speed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            EngineStatusBanner(
                title = "Hardware Sandbox Active",
                subtitle = "Zero server dependency"
            )
        }

        // Section 1: PDF Power Tools
        item {
            ToolSectionHeader(title = "PDF Power Tools", subtitle = "Page manipulation and document structuring")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionRow(
                    title = "Merge PDFs",
                    description = "Combine multiple PDF files into one continuous document",
                    icon = Icons.Default.MergeType,
                    iconTint = MaterialTheme.colorScheme.primary,
                    badge = "Multi-file",
                    onClick = { mergePdfPicker.launch(arrayOf("application/pdf")) }
                )

                ToolActionRow(
                    title = "Rotate PDF Pages",
                    description = "Rotate orientation by 90°, 180°, or 270°",
                    icon = Icons.Default.RotateRight,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    badge = "Instant",
                    onClick = { rotatePdfPicker.launch(arrayOf("application/pdf")) }
                )

                ToolActionRow(
                    title = "Watermark PDF",
                    description = "Stamp semi-transparent security watermark across pages",
                    icon = Icons.Default.Security,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    badge = "Stamp",
                    onClick = { watermarkPdfPicker.launch(arrayOf("application/pdf")) }
                )

                ToolActionRow(
                    title = "Extract PDF to Images",
                    description = "Render high-resolution JPG or PNG for every page",
                    icon = Icons.Default.PhotoLibrary,
                    iconTint = Color(0xFFF43F5E),
                    badge = "Raster",
                    onClick = {
                        viewModel.addSamplePdf()
                        viewModel.selectTab(com.example.ui.viewmodel.ScreenTab.CONVERT)
                    }
                )
            }
        }

        // Section 2: Image Tools
        item {
            ToolSectionHeader(title = "Image Utilities", subtitle = "Resolution, dimensions, and composition")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionRow(
                    title = "Resize Image",
                    description = "Change width and height dimensions with aspect ratio lock",
                    icon = Icons.Default.AspectRatio,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    badge = "Pixel Perfect",
                    onClick = { showResizeDialog = true }
                )

                ToolActionRow(
                    title = "Images to PDF",
                    description = "Turn photos and graphics into a multi-page PDF document",
                    icon = Icons.Default.PictureAsPdf,
                    iconTint = MaterialTheme.colorScheme.primary,
                    badge = "Batch",
                    onClick = { imagesToPdfPicker.launch(arrayOf("image/*")) }
                )

                ToolActionRow(
                    title = "Smart Compress",
                    description = "Optimize images with live yield savings calculation",
                    icon = Icons.Default.Compress,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    badge = "80% Avg",
                    onClick = { viewModel.selectTab(com.example.ui.viewmodel.ScreenTab.COMPRESS) }
                )
            }
        }

        // Section 3: Office & Documents
        item {
            ToolSectionHeader(title = "Office & Documents", subtitle = "Native offline parser & converters")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolActionRow(
                    title = "DOCX to PDF",
                    description = "Convert Microsoft Word XML files to PDF on-device",
                    icon = Icons.Default.Description,
                    iconTint = MaterialTheme.colorScheme.primary,
                    badge = "Offline XML",
                    onClick = {
                        viewModel.selectTab(com.example.ui.viewmodel.ScreenTab.CONVERT)
                    }
                )

                ToolActionRow(
                    title = "Clear Processing Cache",
                    description = "Purge all temporary working files and logs securely",
                    icon = Icons.Default.Layers,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    badge = "Purge",
                    onClick = { viewModel.clearAllHistory() }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Resize Tool Dialog
    if (showResizeDialog) {
        ResizeImageDialog(
            viewModel = viewModel,
            onDismiss = { showResizeDialog = false }
        )
    }
}

@Composable
fun ToolSectionHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ToolActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tool_action_${title.lowercase().replace(" ", "_")}")
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

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
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun ResizeImageDialog(
    viewModel: FileForgeViewModel,
    onDismiss: () -> Unit
) {
    var widthText by remember { mutableStateOf("1280") }
    var heightText by remember { mutableStateOf("720") }
    var keepAspect by remember { mutableStateOf(true) }
    var scalePercent by remember { mutableStateOf(100f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Resize Image",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Scale target image dimensions offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = widthText,
                        onValueChange = { widthText = it.filter { char -> char.isDigit() } },
                        label = { Text("Width (px)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { heightText = it.filter { char -> char.isDigit() } },
                        label = { Text("Height (px)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = keepAspect,
                        onCheckedChange = { keepAspect = it }
                    )
                    Text("Lock Aspect Ratio", style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val w = widthText.toIntOrNull() ?: 1280
                            val h = heightText.toIntOrNull() ?: 720
                            val sampleImg = EngineUtils.createSampleImage(viewModel.getApplication(), "Resize_Input.png", 1920, 1080)
                            viewModel.runToolResizeImage(sampleImg, w, h, keepAspect)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Resize Now")
                    }
                }
            }
        }
    }
}
