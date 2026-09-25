package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Supported image formats for conversion and compression.
 */
enum class SupportedImageFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val supportsAlpha: Boolean
) {
    JPEG("jpg", "image/jpeg", "JPEG (.jpg)", false),
    PNG("png", "image/png", "PNG (.png)", true),
    WEBP("webp", "image/webp", "WebP (.webp)", true),
    WEBP_LOSSY("webp", "image/webp", "WebP Lossy (.webp)", true),
    WEBP_LOSSLESS("webp", "image/webp", "WebP Lossless (.webp)", true);

    fun toCompressFormat(): Bitmap.CompressFormat {
        return when (this) {
            JPEG -> Bitmap.CompressFormat.JPEG
            PNG -> Bitmap.CompressFormat.PNG
            WEBP, WEBP_LOSSY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            WEBP_LOSSLESS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }

    companion object {
        fun fromExtension(ext: String): SupportedImageFormat {
            return when (ext.lowercase().removePrefix(".")) {
                "jpg", "jpeg" -> JPEG
                "png" -> PNG
                "webp" -> WEBP
                else -> JPEG
            }
        }

        fun fromString(format: String): SupportedImageFormat {
            val clean = format.uppercase().trim()
            return when {
                clean.contains("PNG") -> PNG
                clean.contains("WEBP_LOSSLESS") -> WEBP_LOSSLESS
                clean.contains("WEBP_LOSSY") -> WEBP_LOSSY
                clean.contains("WEBP") -> WEBP
                else -> JPEG
            }
        }
    }
}

/**
 * Result of an image format conversion operation.
 */
data class ImageConversionResult(
    val success: Boolean,
    val outputFile: File?,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val sourceFormat: String = "",
    val targetFormat: SupportedImageFormat = SupportedImageFormat.JPEG,
    val message: String = "",
    val details: String = ""
) {
    val spaceSavedBytes: Long
        get() = (originalSizeBytes - outputSizeBytes).coerceAtLeast(0L)

    val savedPercentage: Int
        get() = if (originalSizeBytes > 0 && spaceSavedBytes > 0) {
            ((spaceSavedBytes.toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
        } else {
            0
        }
}

/**
 * Configuration options for the local image compression utility.
 */
data class ImageCompressionOptions(
    val quality: Int = 80, // 1..100
    val maxDimension: Int? = null, // e.g. 1920, 2560 px
    val targetCapBytes: Long? = null, // e.g. 500_000L for 500KB
    val outputFormat: SupportedImageFormat = SupportedImageFormat.JPEG,
    val stripMetadata: Boolean = true,
    val maintainAspectRatio: Boolean = true
)

/**
 * Result of a local image compression operation.
 */
data class ImageCompressionResult(
    val success: Boolean,
    val outputFile: File?,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val compressedWidth: Int = 0,
    val compressedHeight: Int = 0,
    val qualityApplied: Int = 80,
    val message: String = "",
    val details: String = ""
) {
    val spaceSavedBytes: Long
        get() = (originalSizeBytes - outputSizeBytes).coerceAtLeast(0L)

    val savedPercentage: Int
        get() = if (originalSizeBytes > 0 && spaceSavedBytes > 0) {
            ((spaceSavedBytes.toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
        } else {
            0
        }
}

/**
 * High-performance local image processing utility built using standard Android image libraries
 * (Bitmap, BitmapFactory, Canvas, Paint, Matrix, ExifInterface).
 * Operates 100% offline with zero external network traffic.
 */
object ImageProcessingUtils {

    private const val TAG = "ImageProcessingUtils"

    /**
     * Converts image formats (e.g. PNG to JPEG, PNG to WebP, JPEG to PNG, WebP to JPEG)
     * using the standard Android Bitmap API.
     *
     * Handles:
     * - Automatic EXIF orientation normalization so photos remain upright.
     * - Alpha channel transparency handling (flattens onto clean white background when converting to JPEG).
     * - WebP lossy and lossless modes with modern API support.
     */
    suspend fun convertImageFormat(
        context: Context,
        inputFile: File,
        targetFormat: SupportedImageFormat,
        quality: Int = 90,
        customOutputFile: File? = null
    ): ImageConversionResult = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()
        val sourceExt = inputFile.extension.uppercase()

        if (!inputFile.exists() || originalSize == 0L) {
            return@withContext ImageConversionResult(
                success = false,
                outputFile = null,
                originalSizeBytes = originalSize,
                outputSizeBytes = 0L,
                sourceFormat = sourceExt,
                targetFormat = targetFormat,
                message = "Input file does not exist or is empty."
            )
        }

        try {
            // 1. Read EXIF orientation to keep photos upright
            val orientation = getExifOrientation(inputFile)

            // 2. Decode bitmap using BitmapFactory
            val decodeOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions)
                ?: return@withContext ImageConversionResult(
                    success = false,
                    outputFile = null,
                    originalSizeBytes = originalSize,
                    outputSizeBytes = 0L,
                    sourceFormat = sourceExt,
                    targetFormat = targetFormat,
                    message = "Could not decode source image with standard Android BitmapFactory."
                )

            // 3. Apply rotation if needed
            val orientedBitmap = applyExifRotation(rawBitmap, orientation)

            // 4. Handle Alpha transparency: If target doesn't support alpha (like JPEG), composite over white background
            val finalBitmap = if (!targetFormat.supportsAlpha && orientedBitmap.hasAlpha()) {
                val nonAlphaBitmap = Bitmap.createBitmap(
                    orientedBitmap.width,
                    orientedBitmap.height,
                    Bitmap.Config.RGB_565
                )
                val canvas = Canvas(nonAlphaBitmap)
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(orientedBitmap, 0f, 0f, paint)
                if (orientedBitmap != rawBitmap) orientedBitmap.recycle()
                nonAlphaBitmap
            } else {
                orientedBitmap
            }

            // 5. Determine output file
            val outputDir = EngineUtils.getOutputDir(context, "Converted")
            val baseName = inputFile.nameWithoutExtension
            val outFile = customOutputFile ?: File(outputDir, "${baseName}_converted.${targetFormat.extension}")

            // 6. Compress and save to destination using standard Bitmap.compress
            val safeQuality = quality.coerceIn(1, 100)
            FileOutputStream(outFile).use { fos ->
                finalBitmap.compress(targetFormat.toCompressFormat(), safeQuality, fos)
                fos.flush()
            }

            val finalWidth = finalBitmap.width
            val finalHeight = finalBitmap.height

            // Cleanup bitmaps
            if (finalBitmap != rawBitmap) {
                finalBitmap.recycle()
            }
            rawBitmap.recycle()

            val outputSize = outFile.length()
            val details = "Converted $sourceExt → ${targetFormat.displayName} • ${finalWidth}×${finalHeight} px • Quality: $safeQuality%"

            ImageConversionResult(
                success = true,
                outputFile = outFile,
                originalSizeBytes = originalSize,
                outputSizeBytes = outputSize,
                width = finalWidth,
                height = finalHeight,
                sourceFormat = sourceExt,
                targetFormat = targetFormat,
                message = "Converted to ${targetFormat.displayName} successfully",
                details = details
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in convertImageFormat", e)
            ImageConversionResult(
                success = false,
                outputFile = null,
                originalSizeBytes = originalSize,
                outputSizeBytes = 0L,
                sourceFormat = sourceExt,
                targetFormat = targetFormat,
                message = "Conversion error: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Local image compression utility using the Android Bitmap API to reduce image file size
     * while maintaining visual quality.
     *
     * Features:
     * - Sub-sampling with inSampleSize: Reads image bounds first to prevent OutOfMemoryError on large camera photos.
     * - High-fidelity scaling: Maintains aspect ratio and visual sharpness using bilinear filtering.
     * - Dynamic quality optimization: If targetCapBytes is provided, applies binary search compression
     *   to match the size cap while preserving visual quality above strict degradation thresholds.
     * - Standard Android Bitmap.compressFormat (JPEG, WebP, PNG).
     */
    suspend fun compressImageMaintainingQuality(
        context: Context,
        inputFile: File,
        options: ImageCompressionOptions = ImageCompressionOptions()
    ): ImageCompressionResult = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()

        if (!inputFile.exists() || originalSize == 0L) {
            return@withContext ImageCompressionResult(
                success = false,
                outputFile = null,
                originalSizeBytes = originalSize,
                outputSizeBytes = 0L,
                message = "Input image file does not exist or is empty."
            )
        }

        try {
            // 1. Inspect image dimensions without allocating full pixel memory
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(inputFile.absolutePath, boundsOptions)
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight

            if (origW <= 0 || origH <= 0) {
                return@withContext ImageCompressionResult(
                    success = false,
                    outputFile = null,
                    originalSizeBytes = originalSize,
                    outputSizeBytes = 0L,
                    message = "Could not determine image dimensions."
                )
            }

            // 2. Determine target dimensions if maxDimension is specified
            val maxDim = options.maxDimension
            val (targetW, targetH) = if (maxDim != null && maxDim > 0) {
                if (origW > maxDim || origH > maxDim) {
                    val ratio = origW.toFloat() / origH.toFloat()
                    if (origW >= origH) {
                        Pair(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
                    } else {
                        Pair((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
                    }
                } else {
                    Pair(origW, origH)
                }
            } else {
                Pair(origW, origH)
            }

            // 3. Compute optimal inSampleSize for safe memory allocation
            val sampleSize = calculateInSampleSize(boundsOptions, targetW, targetH)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions)
                ?: return@withContext ImageCompressionResult(
                    success = false,
                    outputFile = null,
                    originalSizeBytes = originalSize,
                    outputSizeBytes = 0L,
                    originalWidth = origW,
                    originalHeight = origH,
                    message = "Failed to decode bitmap for compression."
                )

            // 4. Orient according to EXIF data
            val orientation = getExifOrientation(inputFile)
            val orientedBitmap = applyExifRotation(decodedBitmap, orientation)

            // 5. High-quality smooth scaling if dimensions need further refinement
            val finalScaledBitmap = if (orientedBitmap.width != targetW || orientedBitmap.height != targetH) {
                Bitmap.createScaledBitmap(orientedBitmap, targetW, targetH, true)
            } else {
                orientedBitmap
            }

            // 6. Handle alpha for non-alpha formats
            val bitmapToCompress = if (!options.outputFormat.supportsAlpha && finalScaledBitmap.hasAlpha()) {
                val flat = Bitmap.createBitmap(finalScaledBitmap.width, finalScaledBitmap.height, Bitmap.Config.RGB_565)
                val canvas = Canvas(flat)
                canvas.drawColor(Color.WHITE)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                canvas.drawBitmap(finalScaledBitmap, 0f, 0f, paint)
                if (finalScaledBitmap != orientedBitmap && finalScaledBitmap != decodedBitmap) finalScaledBitmap.recycle()
                flat
            } else {
                finalScaledBitmap
            }

            // 7. Dynamic Compression using Android Bitmap API
            val outputDir = EngineUtils.getOutputDir(context, "Compressed")
            val baseName = inputFile.nameWithoutExtension
            val ext = options.outputFormat.extension
            val outFile = File(outputDir, "${baseName}_compressed.$ext")

            val targetCap = options.targetCapBytes
            var finalQuality = options.quality.coerceIn(20, 100)
            var compressedBytes = ByteArrayOutputStream()

            val compressFormat = options.outputFormat.toCompressFormat()

            // First pass at specified quality
            bitmapToCompress.compress(compressFormat, finalQuality, compressedBytes)

            // If target file cap is specified and exceeded, step quality down while keeping visual clarity >= 35%
            if (targetCap != null && targetCap > 0L && compressedBytes.size() > targetCap) {
                var low = 35
                var high = finalQuality
                var bestBytes = compressedBytes

                while (low <= high) {
                    val mid = (low + high) / 2
                    val testStream = ByteArrayOutputStream()
                    bitmapToCompress.compress(compressFormat, mid, testStream)

                    if (testStream.size() <= targetCap) {
                        bestBytes = testStream
                        finalQuality = mid
                        low = mid + 5 // Try slightly higher quality under cap
                    } else {
                        high = mid - 5 // Must reduce quality
                    }
                }
                compressedBytes = bestBytes
            }

            // Write to disk
            FileOutputStream(outFile).use { fos ->
                fos.write(compressedBytes.toByteArray())
                fos.flush()
            }

            val finalW = bitmapToCompress.width
            val finalH = bitmapToCompress.height

            // Cleanup bitmaps
            if (bitmapToCompress != finalScaledBitmap) bitmapToCompress.recycle()
            if (finalScaledBitmap != orientedBitmap) finalScaledBitmap.recycle()
            if (orientedBitmap != decodedBitmap) orientedBitmap.recycle()
            decodedBitmap.recycle()

            val finalSize = outFile.length()
            val details = "Resolution: ${finalW}×${finalH} px • Quality: $finalQuality% • Format: ${options.outputFormat.displayName}"

            ImageCompressionResult(
                success = true,
                outputFile = outFile,
                originalSizeBytes = originalSize,
                outputSizeBytes = finalSize,
                originalWidth = origW,
                originalHeight = origH,
                compressedWidth = finalW,
                compressedHeight = finalH,
                qualityApplied = finalQuality,
                message = "Image compressed successfully",
                details = details
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in compressImageMaintainingQuality", e)
            ImageCompressionResult(
                success = false,
                outputFile = null,
                originalSizeBytes = originalSize,
                outputSizeBytes = 0L,
                message = "Compression failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Calculates the largest inSampleSize value that is a power of 2 and keeps both
     * height and width larger than the requested height and width.
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Reads standard EXIF orientation tag from image file.
     */
    fun getExifOrientation(file: File): Int {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    /**
     * Applies standard rotation and flip matrix according to EXIF orientation.
     */
    fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }

        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM while rotating bitmap, returning unrotated", e)
            bitmap
        }
    }

    /**
     * Saves a processed file directly to the device's public media gallery using MediaStore API.
     * Uses Scoped Storage on Android 10+ (Q+) to save into Pictures/subfolder without requiring runtime permissions.
     */
    suspend fun saveImageToMediaStore(
        context: Context,
        file: File,
        subfolder: String = "FileForge"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext Result.failure(IOException("File does not exist or is empty: ${file.absolutePath}"))
            }

            val extension = file.extension.lowercase()
            val mimeType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                "pdf" -> "application/pdf"
                else -> "image/jpeg"
            }

            val isImage = mimeType.startsWith("image/")

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.SIZE, file.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val relativePath = if (isImage) {
                        Environment.DIRECTORY_PICTURES + File.separator + subfolder
                    } else {
                        Environment.DIRECTORY_DOWNLOADS + File.separator + subfolder
                    }
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collectionUri = if (isImage) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Files.getContentUri("external")
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(IOException("Failed to create MediaStore entry for ${file.name}"))

            resolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
                outputStream.flush()
            } ?: return@withContext Result.failure(IOException("Failed to open output stream for MediaStore Uri"))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            // Media scanner indexing for immediate gallery refresh
            try {
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            } catch (_: Exception) {}

            Log.i(TAG, "Saved ${file.name} to MediaStore: $uri")
            Result.success(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file to MediaStore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Batch saves multiple processed files to the device's public media gallery.
     */
    suspend fun saveImagesToMediaStore(
        context: Context,
        files: List<File>,
        subfolder: String = "FileForge"
    ): List<Uri> = withContext(Dispatchers.IO) {
        val savedUris = mutableListOf<Uri>()
        for (file in files) {
            val result = saveImageToMediaStore(context, file, subfolder)
            result.getOrNull()?.let { savedUris.add(it) }
        }
        savedUris
    }
}
