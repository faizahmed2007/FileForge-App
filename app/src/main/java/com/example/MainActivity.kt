package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.ui.components.FileForgeTopBar
import com.example.ui.components.ProcessingModal
import com.example.ui.components.ResultSummaryDialog
import com.example.ui.screens.CompressScreen
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.FileForgeTheme
import com.example.ui.theme.MonoFont
import com.example.ui.viewmodel.FileForgeViewModel
import com.example.ui.viewmodel.ScreenTab
import java.io.File
import java.util.ArrayList

class MainActivity : ComponentActivity() {

    private val viewModel: FileForgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FileForgeTheme {
                val isSignedInOrGuest by viewModel.isSignedInOrGuest.collectAsState()
                val currentTab by viewModel.currentTab.collectAsState()
                val isProcessing by viewModel.isProcessing.collectAsState()
                val progress by viewModel.progress.collectAsState()
                val progressMessage by viewModel.progressMessage.collectAsState()
                val processingSummary by viewModel.processingSummary.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }
                var showPrivacyInfoDialog by remember { mutableStateOf(false) }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearToast()
                    }
                }

                if (!isSignedInOrGuest) {
                    SignInScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        val subtitle = when (currentTab) {
                            ScreenTab.DASHBOARD -> "Offline Suite"
                            ScreenTab.COMPRESS -> "Compress"
                            ScreenTab.CONVERT -> "Convert"
                            ScreenTab.IMAGE_STUDIO -> "AI Studio"
                            ScreenTab.TOOLS -> "Toolbox"
                        }
                        FileForgeTopBar(
                            subtitle = subtitle,
                            onProfileClick = { showPrivacyInfoDialog = true }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ScreenTab.DASHBOARD,
                                onClick = { viewModel.selectTab(ScreenTab.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "Dashboard"
                                    )
                                },
                                label = { Text("Dashboard", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.COMPRESS,
                                onClick = { viewModel.selectTab(ScreenTab.COMPRESS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.FolderZip,
                                        contentDescription = "Compress"
                                    )
                                },
                                label = { Text("Compress", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_compress")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.CONVERT,
                                onClick = { viewModel.selectTab(ScreenTab.CONVERT) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.SyncAlt,
                                        contentDescription = "Convert"
                                    )
                                },
                                label = { Text("Convert", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_convert")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.IMAGE_STUDIO,
                                onClick = { viewModel.selectTab(ScreenTab.IMAGE_STUDIO) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Studio"
                                    )
                                },
                                label = { Text("AI Studio", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_ai_studio")
                            )

                            NavigationBarItem(
                                selected = currentTab == ScreenTab.TOOLS,
                                onClick = { viewModel.selectTab(ScreenTab.TOOLS) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Tools"
                                    )
                                },
                                label = { Text("Tools", fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tools")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "screen_transition"
                        ) { tab ->
                            when (tab) {
                                ScreenTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                                ScreenTab.COMPRESS -> CompressScreen(viewModel = viewModel)
                                ScreenTab.CONVERT -> ConvertScreen(viewModel = viewModel)
                                ScreenTab.IMAGE_STUDIO -> ImageStudioScreen(viewModel = viewModel)
                                ScreenTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                            }
                        }

                        // Processing Modal
                        if (isProcessing) {
                            ProcessingModal(
                                progress = progress,
                                message = progressMessage
                            )
                        }

                        // Result Summary Dialog
                        processingSummary?.let { summary ->
                            ResultSummaryDialog(
                                summary = summary,
                                onDismiss = { viewModel.dismissResultSummary() },
                                onShare = { shareOutputFiles(summary.outputFiles) },
                                onSaveToGallery = { viewModel.saveFilesToGallery(summary.outputFiles) }
                            )
                        }

                        // Account & Privacy Dialog
                        if (showPrivacyInfoDialog) {
                            AccountAndPrivacyDialog(
                                viewModel = viewModel,
                                onDismiss = { showPrivacyInfoDialog = false },
                                onSignInClick = { viewModel.signInWithGoogle(this@MainActivity) }
                            )
                        }
                    }
                }
            }
        }
    }
    }

    private fun shareOutputFiles(files: List<File>) {
        if (files.isEmpty()) return
        try {
            val uris = ArrayList(files.map {
                FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", it)
            })

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    putExtra(Intent.EXTRA_STREAM, uris[0])
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            startActivity(Intent.createChooser(intent, "Share Processed Files via FileForge"))
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to share files: ${e.message}", e)
        }
    }
}

@Composable
fun AccountAndPrivacyDialog(
    viewModel: FileForgeViewModel,
    onDismiss: () -> Unit,
    onSignInClick: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val authLoading by viewModel.authLoading.collectAsState()
    val cloudCount by viewModel.cloudHistoryCount.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentUser != null) Icons.Default.Person else Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (currentUser != null) "Google Account Connected" else "Account & Cloud Sync",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Firebase Auth & Cloud Sync Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "FIREBASE AUTH & FIRESTORE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        if (currentUser != null) {
                            Text(
                                text = "User: ${currentUser?.displayName ?: "Google User"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Email: ${currentUser?.email ?: "No email"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Firestore: $cloudCount items backed up",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = MonoFont,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.signOut()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_sign_out")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text("Sign Out", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.returnToSignIn()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_switch_account")
                                ) {
                                    Text("Sign-In Page", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "Sign in with Google to securely identify yourself, back up processing history, and persist AI image creations across devices with Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onSignInClick,
                                enabled = !authLoading,
                                modifier = Modifier.fillMaxWidth().testTag("btn_google_signin"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                if (authLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Signing in...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign In with Google", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.returnToSignIn()
                                    onDismiss()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Go to Sign-In Page", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "OFFLINE-FIRST ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = MonoFont,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• All compression and conversion tasks run on-device\n• Secure Room database local cache\n• Private, zero data leakage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Wipe All Trash & Data
                OutlinedButton(
                    onClick = {
                        viewModel.clearAllTrashAndData()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_dialog_wipe_all"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Wipe All Trash Data & Reset", fontSize = 12.sp)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
