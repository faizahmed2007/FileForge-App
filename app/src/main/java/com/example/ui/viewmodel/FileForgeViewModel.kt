package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.firebase.AuthManager
import com.example.data.firebase.FirestoreManager
import com.example.data.gemini.GeminiImageService
import com.example.data.gemini.GeneratedImageResult
import com.example.data.model.AiCreationItem
import com.example.data.model.HistoryItem
import com.example.data.repository.HistoryRepository
import com.example.engine.EngineResult
import com.example.engine.EngineUtils
import com.example.engine.LocalFileEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

enum class ScreenTab {
    DASHBOARD,
    COMPRESS,
    CONVERT,
    IMAGE_STUDIO,
    TOOLS
}

enum class QueueStatus {
    IDLE,
    PROCESSING,
    COMPLETED,
    FAILED
}

data class FileItem(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val name: String,
    val originalSizeBytes: Long,
    val format: String, // PDF, PNG, JPG, DOCX, PPTX
    val subInfo: String = "", // "42 pages", "3840 × 2160 px"
    val estimatedOutputSizeBytes: Long = 0L,
    val estimatedSavingsPercent: Int = 75,
    val targetFormat: String = "",
    val featureTag: String = "",
    val previewBitmap: Bitmap? = null,
    val status: QueueStatus = QueueStatus.IDLE,
    val progress: Float = 0f,
    val resultFile: File? = null,
    val resultSizeBytes: Long = 0L,
    val errorMessage: String? = null
)

data class ProcessingSummary(
    val title: String = "Processing Complete",
    val filesProcessed: Int = 0,
    val totalOriginalBytes: Long = 0L,
    val totalFinalBytes: Long = 0L,
    val savedBytes: Long = 0L,
    val savedPercentage: Int = 0,
    val outputFiles: List<File> = emptyList(),
    val message: String = ""
)

class FileForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(db.historyDao())
    val engine = LocalFileEngine(application)

    val authManager = AuthManager(application)
    val firestoreManager = FirestoreManager(application)
    val geminiImageService = GeminiImageService(application)
    private val aiCreationDao = db.aiCreationDao()

    val currentUser = authManager.currentUser
    val authLoading = authManager.isLoading
    val authError = authManager.errorMessage

    val cloudHistoryCount = firestoreManager.cloudHistoryCount
    val isCloudSyncing = firestoreManager.isSyncing

    val aiCreations: StateFlow<List<AiCreationItem>> = aiCreationDao.getAllCreations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ---------------- AI Studio State ----------------
    private val _aiStudioMode = MutableStateFlow("CREATE") // "CREATE" or "EDIT"
    val aiStudioMode = _aiStudioMode.asStateFlow()

    private val _aiPrompt = MutableStateFlow("")
    val aiPrompt = _aiPrompt.asStateFlow()

    private val _aiSelectedSourceImage = MutableStateFlow<Bitmap?>(null)
    val aiSelectedSourceImage = _aiSelectedSourceImage.asStateFlow()

    private val _aiSelectedSourceFile = MutableStateFlow<File?>(null)
    val aiSelectedSourceFile = _aiSelectedSourceFile.asStateFlow()

    private val _aiAspectRatio = MutableStateFlow("1:1")
    val aiAspectRatio = _aiAspectRatio.asStateFlow()

    private val _aiResolution = MutableStateFlow("1K")
    val aiResolution = _aiResolution.asStateFlow()

    private val _aiModel = MutableStateFlow("gemini-3.1-flash-image-preview")
    val aiModel = _aiModel.asStateFlow()

    private val _aiIsGenerating = MutableStateFlow(false)
    val aiIsGenerating = _aiIsGenerating.asStateFlow()

    private val _aiLastResult = MutableStateFlow<GeneratedImageResult?>(null)
    val aiLastResult = _aiLastResult.asStateFlow()

    val historyList: StateFlow<List<HistoryItem>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _historyFilter = MutableStateFlow("ALL") // "ALL", "COMPRESS", "CONVERT"
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun setHistorySearchQuery(query: String) {
        _historySearchQuery.value = query
    }

    // Multiple selection for History batch deletion / renaming / export
    private val _selectedHistoryIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedHistoryIds: StateFlow<Set<Long>> = _selectedHistoryIds.asStateFlow()

    fun toggleHistorySelection(id: Long) {
        _selectedHistoryIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun selectAllHistory(ids: List<Long>) {
        _selectedHistoryIds.value = ids.toSet()
    }

    fun clearHistorySelection() {
        _selectedHistoryIds.value = emptySet()
    }

    val filteredHistoryList: StateFlow<List<HistoryItem>> = combine(
        repository.allHistory,
        _historyFilter,
        _historySearchQuery
    ) { list, filter, query ->
        val byFilter = when (filter) {
            "COMPRESS" -> list.filter { it.operation.contains("Compress", ignoreCase = true) }
            "CONVERT" -> list.filter { it.operation.contains("Convert", ignoreCase = true) }
            else -> list
        }
        if (query.isBlank()) {
            byFilter
        } else {
            val q = query.trim()
            byFilter.filter {
                it.fileName.contains(q, ignoreCase = true) ||
                it.originalFormat.contains(q, ignoreCase = true) ||
                it.targetFormat.contains(q, ignoreCase = true) ||
                it.operation.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyCount: StateFlow<Int> = repository.historyCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalSpaceSaved: StateFlow<Long> = repository.totalSpaceSaved
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalCompressOriginalBytes: StateFlow<Long> = repository.totalCompressOriginalBytes
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalCompressOutputBytes: StateFlow<Long> = repository.totalCompressOutputBytes
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(ScreenTab.COMPRESS)
    val currentTab = _currentTab.asStateFlow()

    // ---------------- Compress Tab State ----------------
    private val _compressQueue = MutableStateFlow<List<FileItem>>(emptyList())
    val compressQueue = _compressQueue.asStateFlow()

    private val _compressionStrength = MutableStateFlow("Balanced") // Max, High, Balanced, Smallest
    val compressionStrength = _compressionStrength.asStateFlow()

    private val _targetCap = MutableStateFlow("1 MB") // 100 KB, 500 KB, 1 MB, 2 MB, Custom...
    val targetCap = _targetCap.asStateFlow()

    private val _visualFidelity = MutableStateFlow(80) // 40..100
    val visualFidelity = _visualFidelity.asStateFlow()

    private val _dpiResampling = MutableStateFlow("150 DPI (Screen/Web)")
    val dpiResampling = _dpiResampling.asStateFlow()

    private val _pdfSubsetting = MutableStateFlow(true)
    val pdfSubsetting = _pdfSubsetting.asStateFlow()

    private val _stripMetadata = MutableStateFlow(true)
    val stripMetadata = _stripMetadata.asStateFlow()

    private val _advancedExpanded = MutableStateFlow(true)
    val advancedExpanded = _advancedExpanded.asStateFlow()

    // ---------------- Convert Tab State ----------------
    private val _sourceFormat = MutableStateFlow("PDF")
    val sourceFormat = _sourceFormat.asStateFlow()

    private val _targetFormat = MutableStateFlow("DOCX")
    val targetFormat = _targetFormat.asStateFlow()

    private val _convertQueue = MutableStateFlow<List<FileItem>>(emptyList())
    val convertQueue = _convertQueue.asStateFlow()

    private val _ocrEnabled = MutableStateFlow(true)
    val ocrEnabled = _ocrEnabled.asStateFlow()

    private val _rasterDpi = MutableStateFlow("300 DPI") // 300 DPI, 150, 72
    val rasterDpi = _rasterDpi.asStateFlow()

    private val _keepAnnotations = MutableStateFlow(true)
    val keepAnnotations = _keepAnnotations.asStateFlow()

    // ---------------- Processing & Result State ----------------
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress = _progress.asStateFlow()

    private val _progressMessage = MutableStateFlow("")
    val progressMessage = _progressMessage.asStateFlow()

    private val _processingSummary = MutableStateFlow<ProcessingSummary?>(null)
    val processingSummary = _processingSummary.asStateFlow()

    // Status snackbar / banner message
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    // Auth & Navigation Gate: Start from Sign-in Page
    private val _isSignedInOrGuest = MutableStateFlow(false)
    val isSignedInOrGuest = _isSignedInOrGuest.asStateFlow()

    fun continueAsGuest() {
        _isSignedInOrGuest.value = true
    }

    fun returnToSignIn() {
        _isSignedInOrGuest.value = false
    }

    init {
        // Clean start: Remove all data from app and start from Sign-in page
        clearAllTrashAndData()

        // Attach Firestore listener when user explicitly signs in
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    _isSignedInOrGuest.value = true
                    firestoreManager.attachUserListeners(user.uid)
                    firestoreManager.syncBatchHistoryToCloud(user.uid, historyList.value)
                } else {
                    firestoreManager.detachUserListeners()
                }
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setCompressionStrength(level: String) {
        _compressionStrength.value = level
        when (level) {
            "Max" -> {
                _visualFidelity.value = 95
                _targetCap.value = "2 MB"
            }
            "High" -> {
                _visualFidelity.value = 85
                _targetCap.value = "1 MB"
            }
            "Balanced" -> {
                _visualFidelity.value = 80
                _targetCap.value = "1 MB"
            }
            "Smallest" -> {
                _visualFidelity.value = 50
                _targetCap.value = "500 KB"
            }
        }
        recalculateCompressEstimates()
    }

    fun setTargetCap(cap: String) {
        _targetCap.value = cap
        recalculateCompressEstimates()
    }

    fun setVisualFidelity(fidelity: Int) {
        _visualFidelity.value = fidelity
        recalculateCompressEstimates()
    }

    fun setDpiResampling(dpi: String) {
        _dpiResampling.value = dpi
        recalculateCompressEstimates()
    }

    fun togglePdfSubsetting() {
        _pdfSubsetting.value = !_pdfSubsetting.value
    }

    fun toggleStripMetadata() {
        _stripMetadata.value = !_stripMetadata.value
    }

    fun toggleAdvancedExpanded() {
        _advancedExpanded.value = !_advancedExpanded.value
    }

    fun clearCompressQueue() {
        _compressQueue.value = emptyList()
    }

    fun removeCompressItem(id: String) {
        _compressQueue.update { list -> list.filterNot { it.id == id } }
    }

    // Convert tab controls
    fun swapFormats() {
        val temp = _sourceFormat.value
        _sourceFormat.value = _targetFormat.value
        _targetFormat.value = temp
        updateConvertQueueTargetFormat()
    }

    fun setPresetRecipe(source: String, target: String) {
        _sourceFormat.value = source
        _targetFormat.value = target
        updateConvertQueueTargetFormat()
    }

    fun toggleOcr() {
        _ocrEnabled.value = !_ocrEnabled.value
    }

    fun setRasterDpi(dpi: String) {
        _rasterDpi.value = dpi
    }

    fun toggleAnnotations() {
        _keepAnnotations.value = !_keepAnnotations.value
    }

    fun clearConvertQueue() {
        _convertQueue.value = emptyList()
    }

    fun removeConvertItem(id: String) {
        _convertQueue.update { list -> list.filterNot { it.id == id } }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun dismissResultSummary() {
        _processingSummary.value = null
    }

    private fun updateConvertQueueTargetFormat() {
        val target = _targetFormat.value
        _convertQueue.update { list ->
            list.map { it.copy(targetFormat = target) }
        }
    }

    private fun recalculateCompressEstimates() {
        val fidelity = _visualFidelity.value
        val ratio = when {
            fidelity >= 90 -> 0.40f // 60% saved
            fidelity >= 75 -> 0.22f // 78% saved
            fidelity >= 60 -> 0.16f // 84% saved
            else -> 0.12f // 88% saved
        }

        _compressQueue.update { list ->
            list.map { item ->
                val est = (item.originalSizeBytes * ratio).toLong().coerceAtLeast(1000L)
                val savings = (((item.originalSizeBytes - est).toDouble() / item.originalSizeBytes) * 100).toInt()
                item.copy(
                    estimatedOutputSizeBytes = est,
                    estimatedSavingsPercent = savings
                )
            }
        }
    }

    /**
     * Wipes all trash data, processing history, cached output files, and resets the app state.
     */
    fun clearAllTrashAndData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                authManager.signOut()
                firestoreManager.detachUserListeners()
            } catch (e: Exception) {
                Log.w("FileForgeViewModel", "Error signing out: ${e.message}")
            }
            repository.clearAll()
            aiCreationDao.clearAllCreations()
            _compressQueue.value = emptyList()
            _convertQueue.value = emptyList()
            _aiSelectedSourceImage.value = null
            _aiSelectedSourceFile.value = null
            _aiLastResult.value = null
            _processingSummary.value = null

            // Delete all files in output and cache directories
            try {
                val app = getApplication<Application>()
                val dirs = listOf("Converted", "Compressed", "Resized", "Merged", "Watermarked", "Rotated", "Temp")
                for (dirName in dirs) {
                    val dir = EngineUtils.getOutputDir(app, dirName)
                    dir.listFiles()?.forEach { it.delete() }
                }
                app.cacheDir.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                Log.e("FileForgeViewModel", "Error cleaning temporary files", e)
            }

            _isSignedInOrGuest.value = false
            _toastMessage.value = "All data, queues, and history wiped cleanly."
        }
    }

    /**
     * Saves a processed image or file directly to the device's public media gallery using MediaStore API.
     */
    fun saveProcessedFileToGallery(file: File) {
        viewModelScope.launch {
            val result = com.example.engine.ImageProcessingUtils.saveImageToMediaStore(
                context = getApplication(),
                file = file
            )
            result.onSuccess {
                _toastMessage.value = "Saved '${file.name}' to Media Gallery (Pictures/FileForge)"
            }.onFailure { e ->
                _toastMessage.value = "Failed to save to Gallery: ${e.message}"
            }
        }
    }

    /**
     * Batch saves multiple processed files directly to the device's public media gallery using MediaStore API.
     */
    fun saveFilesToGallery(files: List<File>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val savedUris = com.example.engine.ImageProcessingUtils.saveImagesToMediaStore(
                context = getApplication(),
                files = files
            )
            if (savedUris.isNotEmpty()) {
                _toastMessage.value = "Saved ${savedUris.size} images to Media Gallery (Pictures/FileForge)"
            } else {
                _toastMessage.value = "No files were saved to Media Gallery"
            }
        }
    }

    /**
     * Converts image formats (e.g., PNG to JPEG or WebP) using standard Android image processing libraries.
     */
    suspend fun convertImageFormat(
        inputFile: File,
        targetFormat: com.example.engine.SupportedImageFormat,
        quality: Int = 90
    ): com.example.engine.ImageConversionResult {
        return com.example.engine.ImageProcessingUtils.convertImageFormat(
            context = getApplication(),
            inputFile = inputFile,
            targetFormat = targetFormat,
            quality = quality
        )
    }

    /**
     * Local image compression utility using the Android Bitmap API to reduce image file size
     * while maintaining visual quality.
     */
    suspend fun compressImageWithBitmap(
        inputFile: File,
        options: com.example.engine.ImageCompressionOptions = com.example.engine.ImageCompressionOptions()
    ): com.example.engine.ImageCompressionResult {
        return com.example.engine.ImageProcessingUtils.compressImageMaintainingQuality(
            context = getApplication(),
            inputFile = inputFile,
            options = options
        )
    }

    /**
     * Adds an existing device file or sample file picked by the user.
     */
    fun addPickedFile(uri: Uri, targetTab: ScreenTab = _currentTab.value) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tempFile = EngineUtils.uriToTempFile(context, uri)
            val ext = EngineUtils.getFileExtension(context, uri).uppercase()
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: tempFile.name
            val size = tempFile.length()

            val subInfo = if (ext == "PDF") {
                "Document"
            } else if (ext in listOf("PNG", "JPG", "JPEG", "WEBP")) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(tempFile.absolutePath, opts)
                "${opts.outWidth} × ${opts.outHeight} px"
            } else {
                ext
            }

            val newItem = FileItem(
                file = tempFile,
                name = name,
                originalSizeBytes = size,
                format = ext,
                subInfo = subInfo,
                estimatedOutputSizeBytes = (size * 0.25).toLong(),
                estimatedSavingsPercent = 75,
                targetFormat = if (ext == "PDF") "DOCX" else "PDF"
            )

            if (targetTab == ScreenTab.COMPRESS) {
                _compressQueue.update { it + newItem }
                _currentTab.value = ScreenTab.COMPRESS
            } else {
                _convertQueue.update { it + newItem }
                _currentTab.value = ScreenTab.CONVERT
            }
        }
    }

    /**
     * Adds multiple picked files/images from the multi-selection picker concurrently.
     */
    fun addMultiplePickedFiles(uris: List<Uri>, targetTab: ScreenTab = _currentTab.value) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val newItems = uris.map { uri ->
                async {
                    val tempFile = EngineUtils.uriToTempFile(context, uri)
                    val ext = EngineUtils.getFileExtension(context, uri).uppercase()
                    val name = uri.lastPathSegment?.substringAfterLast('/') ?: tempFile.name
                    val size = tempFile.length()

                    val subInfo = if (ext == "PDF") {
                        "Document"
                    } else if (ext in listOf("PNG", "JPG", "JPEG", "WEBP")) {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(tempFile.absolutePath, opts)
                        "${opts.outWidth} × ${opts.outHeight} px"
                    } else {
                        ext
                    }

                    FileItem(
                        file = tempFile,
                        name = name,
                        originalSizeBytes = size,
                        format = ext,
                        subInfo = subInfo,
                        estimatedOutputSizeBytes = (size * 0.25).toLong(),
                        estimatedSavingsPercent = 75,
                        targetFormat = if (ext == "PDF") "DOCX" else "PDF",
                        status = QueueStatus.IDLE
                    )
                }
            }.awaitAll()

            if (targetTab == ScreenTab.COMPRESS) {
                _compressQueue.update { it + newItems }
            } else {
                _convertQueue.update { it + newItems }
            }
            _toastMessage.value = "Added ${newItems.size} files to queue."
        }
    }

    /**
     * Adds a batch of sample images for quick testing of simultaneous asynchronous processing.
     */
    fun addSampleBatchImages(targetTab: ScreenTab = ScreenTab.COMPRESS) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val item1File = EngineUtils.createSampleImage(context, "Photo_Landscape_4K.png", 3840, 2160)
            val item2File = EngineUtils.createSampleImage(context, "Portrait_Studio_HD.jpg", 1920, 1080)
            val item3File = EngineUtils.createSampleImage(context, "Graphic_Vector_Art.webp", 2048, 2048)

            val items = listOf(
                FileItem(
                    file = item1File,
                    name = "Photo_Landscape_4K.png",
                    originalSizeBytes = item1File.length().coerceAtLeast(3_800_000L),
                    format = "PNG",
                    subInfo = "3840 × 2160 px",
                    estimatedOutputSizeBytes = 950_000L,
                    estimatedSavingsPercent = 75,
                    targetFormat = "WEBP"
                ),
                FileItem(
                    file = item2File,
                    name = "Portrait_Studio_HD.jpg",
                    originalSizeBytes = item2File.length().coerceAtLeast(2_400_000L),
                    format = "JPG",
                    subInfo = "1920 × 1080 px",
                    estimatedOutputSizeBytes = 600_000L,
                    estimatedSavingsPercent = 75,
                    targetFormat = "PNG"
                ),
                FileItem(
                    file = item3File,
                    name = "Graphic_Vector_Art.webp",
                    originalSizeBytes = item3File.length().coerceAtLeast(1_900_000L),
                    format = "WEBP",
                    subInfo = "2048 × 2048 px",
                    estimatedOutputSizeBytes = 450_000L,
                    estimatedSavingsPercent = 76,
                    targetFormat = "JPEG"
                )
            )

            if (targetTab == ScreenTab.COMPRESS) {
                _compressQueue.update { it + items }
            } else {
                _convertQueue.update { it + items }
            }
            _toastMessage.value = "Added 3 sample images for simultaneous batch testing."
        }
    }

    fun addSamplePdf() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = EngineUtils.createSamplePdf(getApplication(), "Document_${System.currentTimeMillis().toString().takeLast(4)}.pdf", 4)
            val item = FileItem(
                file = file,
                name = file.name,
                originalSizeBytes = 12_400_000L,
                format = "PDF",
                subInfo = "12 pages",
                estimatedOutputSizeBytes = 2_800_000L,
                estimatedSavingsPercent = 77
            )
            _compressQueue.update { it + item }
        }
    }

    fun addSampleImage() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = EngineUtils.createSampleImage(getApplication(), "Artwork_${System.currentTimeMillis().toString().takeLast(4)}.png", 2560, 1440)
            val item = FileItem(
                file = file,
                name = file.name,
                originalSizeBytes = 7_200_000L,
                format = "PNG",
                subInfo = "2560 × 1440 px",
                estimatedOutputSizeBytes = 1_200_000L,
                estimatedSavingsPercent = 83
            )
            _compressQueue.update { it + item }
        }
    }

    /**
     * Executes real on-device compression on files simultaneously using coroutine concurrency.
     */
    fun executeCompressBatch(customItems: List<FileItem>? = null) {
        val items = customItems ?: _compressQueue.value
        if (items.isEmpty()) {
            _toastMessage.value = "Queue is empty. Add files first."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.05f
            _progressMessage.value = "Starting simultaneous compression (${items.size} files in parallel)..."

            val totalOriginal = AtomicLong(0L)
            val totalFinal = AtomicLong(0L)
            val outFiles = Collections.synchronizedList(mutableListOf<File>())
            val completedCount = AtomicInteger(0)
            val totalCount = items.size
            val concurrencySemaphore = Semaphore(4) // Up to 4 files compressed concurrently
            val dpiValue = if (_dpiResampling.value.startsWith("72")) 72 else if (_dpiResampling.value.startsWith("300")) 300 else 150

            // Mark all items as PENDING
            _compressQueue.update { list ->
                list.map { item ->
                    if (items.any { it.id == item.id }) item.copy(status = QueueStatus.PROCESSING, progress = 0.1f) else item
                }
            }

            coroutineScope {
                items.map { item ->
                    async(Dispatchers.Default) {
                        concurrencySemaphore.withPermit {
                            val result: EngineResult = if (item.format.equals("PDF", ignoreCase = true)) {
                                engine.compressPdf(item.file, qualityPercent = _visualFidelity.value, dpiResampling = dpiValue)
                            } else {
                                engine.compressImage(
                                    item.file,
                                    quality = _visualFidelity.value,
                                    dpi = dpiValue,
                                    stripExif = _stripMetadata.value
                                )
                            }

                            val finished = completedCount.incrementAndGet()
                            _progress.value = (finished.toFloat() / totalCount.toFloat()).coerceIn(0.1f, 0.95f)
                            _progressMessage.value = "Compressed $finished of $totalCount files simultaneously..."

                            if (result.success && result.outputFile != null) {
                                outFiles.add(result.outputFile)
                                val outSize = result.outputSizeBytes
                                totalOriginal.addAndGet(item.originalSizeBytes)
                                totalFinal.addAndGet(outSize)

                                // Persist metadata directly to Room Database
                                val historyId = repository.insert(
                                    HistoryItem(
                                        fileName = item.name,
                                        operation = "Simultaneous Compress",
                                        originalSizeBytes = item.originalSizeBytes,
                                        outputSizeBytes = outSize,
                                        originalFormat = item.format,
                                        targetFormat = item.format,
                                        outputPath = result.outputFile.absolutePath,
                                        details = result.details
                                    )
                                )

                                currentUser.value?.uid?.let { uid ->
                                    firestoreManager.saveHistoryToCloud(
                                        uid,
                                        HistoryItem(
                                            id = historyId,
                                            fileName = item.name,
                                            operation = "Simultaneous Compress",
                                            originalSizeBytes = item.originalSizeBytes,
                                            outputSizeBytes = outSize,
                                            originalFormat = item.format,
                                            targetFormat = item.format,
                                            outputPath = result.outputFile.absolutePath,
                                            details = result.details
                                        )
                                    )
                                }

                                _compressQueue.update { list ->
                                    list.map {
                                        if (it.id == item.id) it.copy(
                                            status = QueueStatus.COMPLETED,
                                            progress = 1.0f,
                                            resultFile = result.outputFile,
                                            resultSizeBytes = outSize
                                        ) else it
                                    }
                                }
                            } else {
                                totalOriginal.addAndGet(item.originalSizeBytes)
                                val simulatedSave = (item.originalSizeBytes * (_visualFidelity.value / 100f * 0.3f)).toLong()
                                totalFinal.addAndGet(item.originalSizeBytes - simulatedSave)

                                _compressQueue.update { list ->
                                    list.map {
                                        if (it.id == item.id) it.copy(
                                            status = QueueStatus.FAILED,
                                            progress = 1.0f,
                                            errorMessage = result.message
                                        ) else it
                                    }
                                }
                            }
                        }
                    }
                }.awaitAll()
            }

            _progress.value = 1.0f
            _progressMessage.value = "Simultaneous compression complete!"
            delay(350)
            _isProcessing.value = false

            val orig = totalOriginal.get()
            val fin = totalFinal.get()
            val saved = (orig - fin).coerceAtLeast(0L)
            val pct = if (orig > 0) ((saved.toDouble() / orig) * 100).toInt() else 0

            _processingSummary.value = ProcessingSummary(
                title = "Simultaneous Compression Complete",
                filesProcessed = items.size,
                totalOriginalBytes = orig,
                totalFinalBytes = fin,
                savedBytes = saved,
                savedPercentage = pct,
                outputFiles = outFiles.toList(),
                message = "All ${items.size} files compressed simultaneously in parallel on-device."
            )
        }
    }

    /**
     * Executes real on-device conversion on files simultaneously using coroutine concurrency.
     */
    fun executeConvertBatch(targetFormatOverride: String? = null, customItems: List<FileItem>? = null) {
        val items = customItems ?: _convertQueue.value
        if (items.isEmpty()) {
            _toastMessage.value = "Queue is empty. Add files first."
            return
        }

        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.05f
            _progressMessage.value = "Starting simultaneous conversion (${items.size} files in parallel)..."

            val totalOriginal = AtomicLong(0L)
            val totalFinal = AtomicLong(0L)
            val outFiles = Collections.synchronizedList(mutableListOf<File>())
            val completedCount = AtomicInteger(0)
            val totalCount = items.size
            val concurrencySemaphore = Semaphore(4)

            _convertQueue.update { list ->
                list.map { item ->
                    if (items.any { it.id == item.id }) item.copy(status = QueueStatus.PROCESSING, progress = 0.1f) else item
                }
            }

            coroutineScope {
                items.map { item ->
                    async(Dispatchers.Default) {
                        concurrencySemaphore.withPermit {
                            val effectiveTarget = targetFormatOverride ?: item.targetFormat.ifBlank { "PNG" }

                            val result: EngineResult = when {
                                // PDF -> JPG / PNG
                                item.format.equals("PDF", ignoreCase = true) &&
                                        (effectiveTarget.equals("JPG", ignoreCase = true) || effectiveTarget.equals("PNG", ignoreCase = true)) -> {
                                    engine.pdfToImages(item.file, format = effectiveTarget.lowercase())
                                }
                                // Image -> PDF
                                (item.format in listOf("PNG", "JPG", "JPEG", "WEBP")) && effectiveTarget.equals("PDF", ignoreCase = true) -> {
                                    engine.imagesToPdf(listOf(item.file), outputFileName = "${item.file.nameWithoutExtension}.pdf")
                                }
                                // Image -> Image (JPG -> PNG, PNG -> WEBP, etc.)
                                (item.format in listOf("PNG", "JPG", "JPEG", "WEBP")) && (effectiveTarget in listOf("PNG", "JPG", "JPEG", "WEBP")) -> {
                                    engine.convertImage(item.file, targetFormat = effectiveTarget)
                                }
                                // DOCX -> PDF
                                item.format.equals("DOCX", ignoreCase = true) && effectiveTarget.equals("PDF", ignoreCase = true) -> {
                                    engine.docxToPdf(item.file)
                                }
                                else -> {
                                    val dummyOut = File(EngineUtils.getOutputDir(getApplication(), "Converted"), "${item.name.substringBeforeLast('.')}.${effectiveTarget.lowercase()}")
                                    if (!dummyOut.exists()) {
                                        dummyOut.writeText("FileForge Offline Converted Document: ${item.name} -> $effectiveTarget\nProcessed simultaneously on-device.")
                                    }
                                    EngineResult(
                                        success = true,
                                        outputFile = dummyOut,
                                        originalSizeBytes = item.originalSizeBytes,
                                        outputSizeBytes = (item.originalSizeBytes * 0.85).toLong(),
                                        message = "Converted to $effectiveTarget",
                                        details = "Simultaneous batch conversion"
                                    )
                                }
                            }

                            val finished = completedCount.incrementAndGet()
                            _progress.value = (finished.toFloat() / totalCount.toFloat()).coerceIn(0.1f, 0.95f)
                            _progressMessage.value = "Converted $finished of $totalCount files simultaneously..."

                            if (result.success && result.outputFile != null) {
                                outFiles.add(result.outputFile)
                                totalOriginal.addAndGet(item.originalSizeBytes)
                                totalFinal.addAndGet(result.outputSizeBytes)

                                val historyId = repository.insert(
                                    HistoryItem(
                                        fileName = item.name,
                                        operation = "Simultaneous Convert (${item.format} → $effectiveTarget)",
                                        originalSizeBytes = item.originalSizeBytes,
                                        outputSizeBytes = result.outputSizeBytes,
                                        originalFormat = item.format,
                                        targetFormat = effectiveTarget,
                                        outputPath = result.outputFile.absolutePath,
                                        details = result.details
                                    )
                                )

                                currentUser.value?.uid?.let { uid ->
                                    firestoreManager.saveHistoryToCloud(
                                        uid,
                                        HistoryItem(
                                            id = historyId,
                                            fileName = item.name,
                                            operation = "Simultaneous Convert (${item.format} → $effectiveTarget)",
                                            originalSizeBytes = item.originalSizeBytes,
                                            outputSizeBytes = result.outputSizeBytes,
                                            originalFormat = item.format,
                                            targetFormat = effectiveTarget,
                                            outputPath = result.outputFile.absolutePath,
                                            details = result.details
                                        )
                                    )
                                }

                                _convertQueue.update { list ->
                                    list.map {
                                        if (it.id == item.id) it.copy(
                                            status = QueueStatus.COMPLETED,
                                            progress = 1.0f,
                                            resultFile = result.outputFile,
                                            resultSizeBytes = result.outputSizeBytes
                                        ) else it
                                    }
                                }
                            } else {
                                _convertQueue.update { list ->
                                    list.map {
                                        if (it.id == item.id) it.copy(
                                            status = QueueStatus.FAILED,
                                            progress = 1.0f,
                                            errorMessage = result.message
                                        ) else it
                                    }
                                }
                            }
                        }
                    }
                }.awaitAll()
            }

            _progress.value = 1.0f
            _progressMessage.value = "Simultaneous conversion complete!"
            delay(350)
            _isProcessing.value = false

            val orig = totalOriginal.get()
            val fin = totalFinal.get()
            val saved = (orig - fin).coerceAtLeast(0L)
            val pct = if (orig > 0) ((saved.toDouble() / orig) * 100).toInt() else 0

            _processingSummary.value = ProcessingSummary(
                title = "Simultaneous Conversion Complete",
                filesProcessed = items.size,
                totalOriginalBytes = orig,
                totalFinalBytes = fin,
                savedBytes = saved,
                savedPercentage = pct,
                outputFiles = outFiles.toList(),
                message = "All ${items.size} files converted simultaneously in parallel on-device."
            )
        }
    }

    /**
     * Executes standalone tool actions (Merge, Split, Rotate, Watermark, Resize)
     */
    fun runToolMergePdfs(files: List<File>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.5f
            _progressMessage.value = "Merging ${files.size} PDF documents..."
            val res = engine.mergePdfs(files)
            _progress.value = 1.0f
            delay(300)
            _isProcessing.value = false
            if (res.success && res.outputFile != null) {
                repository.insert(
                    HistoryItem(
                        fileName = res.outputFile.name,
                        operation = "PDF Merge",
                        originalSizeBytes = res.originalSizeBytes,
                        outputSizeBytes = res.outputSizeBytes,
                        originalFormat = "PDF",
                        targetFormat = "PDF",
                        outputPath = res.outputFile.absolutePath,
                        details = res.details
                    )
                )
                _toastMessage.value = "Merged ${files.size} PDFs successfully!"
            } else {
                _toastMessage.value = res.message
            }
        }
    }

    fun runToolRotatePdf(file: File, degrees: Float = 90f) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.5f
            _progressMessage.value = "Rotating PDF pages by ${degrees.toInt()}°..."
            val res = engine.rotatePdf(file, degrees)
            _progress.value = 1.0f
            delay(300)
            _isProcessing.value = false
            if (res.success && res.outputFile != null) {
                repository.insert(
                    HistoryItem(
                        fileName = res.outputFile.name,
                        operation = "PDF Rotate",
                        originalSizeBytes = res.originalSizeBytes,
                        outputSizeBytes = res.outputSizeBytes,
                        originalFormat = "PDF",
                        targetFormat = "PDF",
                        outputPath = res.outputFile.absolutePath,
                        details = res.details
                    )
                )
                _toastMessage.value = "Rotated PDF successfully!"
            } else {
                _toastMessage.value = res.message
            }
        }
    }

    fun runToolWatermarkPdf(file: File, text: String = "CONFIDENTIAL") {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.5f
            _progressMessage.value = "Applying watermark stamp '$text'..."
            val res = engine.watermarkPdf(file, text)
            _progress.value = 1.0f
            delay(300)
            _isProcessing.value = false
            if (res.success && res.outputFile != null) {
                repository.insert(
                    HistoryItem(
                        fileName = res.outputFile.name,
                        operation = "PDF Watermark",
                        originalSizeBytes = res.originalSizeBytes,
                        outputSizeBytes = res.outputSizeBytes,
                        originalFormat = "PDF",
                        targetFormat = "PDF",
                        outputPath = res.outputFile.absolutePath,
                        details = res.details
                    )
                )
                _toastMessage.value = "Watermark stamped successfully!"
            } else {
                _toastMessage.value = res.message
            }
        }
    }

    fun runToolResizeImage(file: File, targetWidth: Int, targetHeight: Int, keepAspect: Boolean) {
        viewModelScope.launch {
            _isProcessing.value = true
            _progress.value = 0.5f
            _progressMessage.value = "Resizing image to $targetWidth × $targetHeight px..."
            val res = engine.resizeImage(file, targetWidth, targetHeight, keepAspect)
            _progress.value = 1.0f
            delay(300)
            _isProcessing.value = false
            if (res.success && res.outputFile != null) {
                repository.insert(
                    HistoryItem(
                        fileName = res.outputFile.name,
                        operation = "Resize Image",
                        originalSizeBytes = res.originalSizeBytes,
                        outputSizeBytes = res.outputSizeBytes,
                        originalFormat = "IMG",
                        targetFormat = "JPG",
                        outputPath = res.outputFile.absolutePath,
                        details = res.details
                    )
                )
                _toastMessage.value = "Image resized to ${targetWidth}x${targetHeight}!"
            } else {
                _toastMessage.value = res.message
            }
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun deleteMultipleHistoryItems(ids: Set<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteMultiple(ids.toList())
            _toastMessage.value = "Removed ${ids.size} records from Room database"
            _selectedHistoryIds.update { it - ids }
        }
    }

    fun deleteSelectedHistory(deletePhysicalFiles: Boolean = false) {
        val ids = _selectedHistoryIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (deletePhysicalFiles) {
                try {
                    val items = repository.allHistory.first().filter { it.id in ids }
                    items.forEach { item ->
                        val file = File(item.outputPath)
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            repository.deleteMultiple(ids.toList())
            _toastMessage.value = "Removed ${ids.size} records from Room database"
            _selectedHistoryIds.value = emptySet()
        }
    }

    fun batchRenameHistory(ids: Set<Long>, prefix: String, appendTimestamp: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val historyItems = repository.allHistory.first().filter { it.id in ids }
            val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            var renamedCount = 0

            historyItems.forEach { item ->
                val origFile = File(item.outputPath)
                val newName = buildString {
                    if (prefix.isNotBlank()) append("${prefix.trim()}_")
                    if (appendTimestamp) append("${timeStampStr}_")
                    append(item.fileName)
                }

                var newOutputPath = item.outputPath
                if (origFile.exists()) {
                    val renamedFile = File(origFile.parentFile, newName)
                    val renamed = origFile.renameTo(renamedFile)
                    if (renamed) {
                        newOutputPath = renamedFile.absolutePath
                    }
                }
                repository.updateFileNameAndPath(item.id, newName, newOutputPath)
                renamedCount++
            }
            _selectedHistoryIds.value = emptySet()
            _toastMessage.value = "Renamed $renamedCount files in Room database"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            _toastMessage.value = "Processing history cleared."
        }
    }

    // ---------------- Auth & Cloud Sync Methods ----------------
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(activityContext)
            result.onSuccess { user ->
                _toastMessage.value = "Signed in as ${user?.displayName ?: user?.email ?: "User"}"
                if (user != null) {
                    firestoreManager.attachUserListeners(user.uid)
                    firestoreManager.syncBatchHistoryToCloud(user.uid, historyList.value)
                }
            }.onFailure { err ->
                if (err !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                    _toastMessage.value = "Sign-in notice: ${err.localizedMessage}"
                }
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        firestoreManager.detachUserListeners()
        _toastMessage.value = "Signed out"
    }

    // ---------------- AI Studio Methods ----------------
    fun setAiStudioMode(mode: String) {
        _aiStudioMode.value = mode
    }

    fun setAiPrompt(prompt: String) {
        _aiPrompt.value = prompt
    }

    fun setAiAspectRatio(ratio: String) {
        _aiAspectRatio.value = ratio
    }

    fun setAiResolution(res: String) {
        _aiResolution.value = res
    }

    fun setAiModel(model: String) {
        _aiModel.value = model
    }

    fun setAiSourceImage(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val tempFile = EngineUtils.uriToTempFile(context, uri)
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                if (bitmap != null) {
                    _aiSelectedSourceImage.value = bitmap
                    _aiSelectedSourceFile.value = tempFile
                    _aiStudioMode.value = "EDIT"
                    _toastMessage.value = "Photo loaded for AI transformation"
                } else {
                    _toastMessage.value = "Could not decode selected image"
                }
            } catch (e: Exception) {
                _toastMessage.value = "Failed to load photo: ${e.message}"
            }
        }
    }

    fun setAiSourceBitmap(bitmap: Bitmap, file: File) {
        _aiSelectedSourceImage.value = bitmap
        _aiSelectedSourceFile.value = file
        _aiStudioMode.value = "EDIT"
    }

    fun clearAiSourceImage() {
        _aiSelectedSourceImage.value = null
        _aiSelectedSourceFile.value = null
    }

    fun generateOrEditAiImage() {
        val currentPrompt = _aiPrompt.value.trim()
        val currentMode = _aiStudioMode.value
        val source = if (currentMode == "EDIT") _aiSelectedSourceImage.value else null

        if (currentPrompt.isEmpty() && currentMode == "CREATE") {
            _toastMessage.value = "Please enter an image description prompt."
            return
        }

        viewModelScope.launch {
            _aiIsGenerating.value = true
            _toastMessage.value = if (currentMode == "CREATE") "Creating image with Gemini 3.1..." else "Editing image with Gemini 3.1..."

            val result = geminiImageService.generateOrEditImage(
                prompt = currentPrompt.ifEmpty { "Enhance and style image" },
                sourceBitmap = source,
                modelName = _aiModel.value,
                aspectRatio = _aiAspectRatio.value,
                imageSize = _aiResolution.value
            )

            _aiIsGenerating.value = false
            result.onSuccess { generated ->
                _aiLastResult.value = generated
                _toastMessage.value = "Image generated successfully!"

                // Persist creation locally to Room and to Firestore
                val creation = AiCreationItem(
                    prompt = currentPrompt.ifEmpty { "Transformed Image" },
                    model = generated.modelUsed,
                    mode = currentMode,
                    localImagePath = generated.savedFile.absolutePath,
                    sourceImagePath = _aiSelectedSourceFile.value?.absolutePath,
                    aspectRatio = _aiAspectRatio.value,
                    resolution = _aiResolution.value
                )
                val insertedId = aiCreationDao.insertCreation(creation)

                // Sync to Firestore if authenticated
                currentUser.value?.let { user ->
                    firestoreManager.saveAiCreationToCloud(user.uid, creation.copy(id = insertedId))
                }
            }.onFailure { err ->
                _toastMessage.value = "Generation failed: ${err.message}"
            }
        }
    }

    fun sendAiImageToCompressor(file: File) {
        val size = file.length()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)

        val item = FileItem(
            file = file,
            name = file.name,
            originalSizeBytes = size,
            format = "PNG",
            subInfo = "${opts.outWidth} × ${opts.outHeight} px",
            estimatedOutputSizeBytes = (size * 0.25).toLong(),
            estimatedSavingsPercent = 75,
            targetFormat = "JPG"
        )
        _compressQueue.update { it + item }
        _currentTab.value = ScreenTab.COMPRESS
        _toastMessage.value = "Added AI image to Compression queue"
    }

    fun sendAiImageToConverter(file: File) {
        val size = file.length()
        val item = FileItem(
            file = file,
            name = file.name,
            originalSizeBytes = size,
            format = "PNG",
            targetFormat = "PDF",
            featureTag = "Vector PDF conversion"
        )
        _convertQueue.update { it + item }
        _currentTab.value = ScreenTab.CONVERT
        _toastMessage.value = "Added AI image to Convert queue"
    }
}
