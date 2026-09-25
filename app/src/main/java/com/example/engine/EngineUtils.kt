package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.util.zip.ZipInputStream

object EngineUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        val df = DecimalFormat("#,##0.#")
        return "${df.format(value)} ${units[digitGroups]}"
    }

    fun getOutputDir(context: Context, subFolder: String = "FileForge"): File {
        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outDir = File(baseDir, subFolder)
        if (!outDir.exists()) {
            outDir.mkdirs()
        }
        return outDir
    }

    /**
     * Copies any Uri to a temporary cache file so it can be reliably opened with FileDescriptor.
     */
    fun uriToTempFile(context: Context, uri: Uri, tempNamePrefix: String = "input_"): File {
        val tempFile = File(context.cacheDir, "${tempNamePrefix}${System.currentTimeMillis()}.${getFileExtension(context, uri)}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun getFileExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        return when {
            mime?.contains("pdf", ignoreCase = true) == true -> "pdf"
            mime?.contains("png", ignoreCase = true) == true -> "png"
            mime?.contains("jpeg", ignoreCase = true) == true || mime?.contains("jpg", ignoreCase = true) == true -> "jpg"
            mime?.contains("webp", ignoreCase = true) == true -> "webp"
            mime?.contains("word", ignoreCase = true) == true || mime?.contains("docx", ignoreCase = true) == true -> "docx"
            mime?.contains("sheet", ignoreCase = true) == true || mime?.contains("xlsx", ignoreCase = true) == true -> "xlsx"
            mime?.contains("presentation", ignoreCase = true) == true || mime?.contains("pptx", ignoreCase = true) == true -> "pptx"
            else -> uri.lastPathSegment?.substringAfterLast('.', "") ?: "dat"
        }
    }

    /**
     * Extracts text from DOCX file by reading word/document.xml directly without external cloud APIs.
     */
    fun extractTextFromDocx(inputStream: InputStream): String {
        val stringBuilder = java.lang.StringBuilder()
        try {
            val zip = ZipInputStream(inputStream)
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "word/document.xml") {
                    val xmlContent = zip.bufferedReader().readText()
                    // Extract text between <w:t> and </w:t> tags
                    val regex = Regex("<w:t[^>]*>(.*?)</w:t>")
                    val matches = regex.findAll(xmlContent)
                    for (match in matches) {
                        stringBuilder.append(match.groupValues[1]).append(" ")
                    }
                    break
                }
                entry = zip.nextEntry
            }
            zip.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return stringBuilder.toString().trim()
    }

    /**
     * Creates a sample PDF with real content and pages so the user can test offline features right away.
     */
    fun createSamplePdf(context: Context, fileName: String, pageCount: Int = 4): File {
        val outFile = File(getOutputDir(context, "Samples"), fileName)
        if (outFile.exists() && outFile.length() > 0) return outFile

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()

        val titlePaint = Paint().apply {
            color = Color.rgb(20, 25, 45)
            textSize = 22f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(50, 60, 80)
            textSize = 12f
            isAntiAlias = true
        }

        val accentPaint = Paint().apply {
            color = Color.rgb(99, 102, 241)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(230, 235, 255)
        }

        for (p in 1..pageCount) {
            val page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, p).create())
            val canvas = page.canvas

            // Header banner
            canvas.drawRect(0f, 0f, 595f, 60f, headerPaint)
            accentPaint.color = Color.rgb(99, 102, 241)
            canvas.drawRect(0f, 0f, 595f, 6f, accentPaint)

            canvas.drawText("FILEFORGE OFFLINE SECURE DOCUMENT", 40f, 38f, titlePaint.apply { textSize = 14f })
            canvas.drawText("Page $p of $pageCount", 490f, 38f, bodyPaint)

            // Main Content
            titlePaint.textSize = 20f
            canvas.drawText("Executive Financial Report - Section $p", 40f, 100f, titlePaint)

            bodyPaint.textSize = 11f
            var y = 130f
            val sampleLines = listOf(
                "This document was generated locally on-device without any internet or cloud access.",
                "FileForge Offline guarantees complete privacy and cryptographic isolation for all files.",
                "Quarterly summary: Revenue increased by 24.8% through operational efficiencies.",
                "All sensitive client financial models and projection datasets remain strictly within local sandbox.",
                "Performance metrics indicate zero network telemetry occurred during creation.",
                "Local verification hash: SHA-256 verified hermetic environment.",
                "Key takeaways: Reduced processing latency by 85% compared to online upload conversion tools.",
                "Storage footprint optimization achieved through local vector rasterization."
            )

            for (line in sampleLines) {
                canvas.drawText(line, 40f, y, bodyPaint)
                y += 20f
            }

            // Draw a sample chart illustration on page 1 & 2
            if (p <= 2) {
                y += 20f
                canvas.drawText("Quarterly Metrics Growth (Visual Asset)", 40f, y, titlePaint.apply { textSize = 13f })
                y += 15f
                val barColors = intArrayOf(
                    Color.rgb(99, 102, 241),
                    Color.rgb(16, 185, 129),
                    Color.rgb(245, 158, 11),
                    Color.rgb(129, 140, 248)
                )
                val heights = floatArrayOf(80f, 130f, 170f, 210f)
                val labels = arrayOf("Q1", "Q2", "Q3", "Q4")
                for (i in barColors.indices) {
                    accentPaint.color = barColors[i]
                    val x = 60f + i * 110f
                    canvas.drawRect(x, y + (220f - heights[i]), x + 60f, y + 220f, accentPaint)
                    canvas.drawText(labels[i], x + 20f, y + 240f, bodyPaint)
                }
            }

            // Footer
            canvas.drawLine(40f, 790f, 555f, 790f, Paint().apply { color = Color.LTGRAY })
            canvas.drawText("CONFIDENTIAL • 100% OFFLINE SECURE • FILEFORGE ENGINE", 40f, 810f, bodyPaint.apply { textSize = 9f })

            document.finishPage(page)
        }

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()
        return outFile
    }

    /**
     * Creates a high-res sample image so user can test image compression, resizing, and format conversion.
     */
    fun createSampleImage(context: Context, fileName: String, width: Int = 1920, height: Int = 1080): File {
        val outFile = File(getOutputDir(context, "Samples"), fileName)
        if (outFile.exists() && outFile.length() > 0) return outFile

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Gradient-like backdrop
        val paint = Paint().apply { isAntiAlias = true }
        for (i in 0 until height step 4) {
            val ratio = i.toFloat() / height
            val r = (15 + ratio * 40).toInt()
            val g = (20 + ratio * 60).toInt()
            val b = (60 + ratio * 150).toInt()
            paint.color = Color.rgb(r, g, b)
            canvas.drawRect(0f, i.toFloat(), width.toFloat(), (i + 4).toFloat(), paint)
        }

        // Geometric vibrant art accents matching the Stitch design
        val circlePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        circlePaint.color = Color.argb(180, 99, 102, 241)
        canvas.drawCircle(width * 0.35f, height * 0.45f, 320f, circlePaint)

        circlePaint.color = Color.argb(160, 16, 185, 129)
        canvas.drawCircle(width * 0.65f, height * 0.55f, 260f, circlePaint)

        circlePaint.color = Color.argb(140, 245, 158, 11)
        canvas.drawCircle(width * 0.5f, height * 0.3f, 180f, circlePaint)

        // Title on image
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 72f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("FileForge Offline Studio", 100f, height - 160f, textPaint)

        val subTextPaint = Paint().apply {
            color = Color.rgb(200, 210, 240)
            textSize = 36f
            isAntiAlias = true
        }
        canvas.drawText("High Fidelity $width × $height Master Asset", 100f, height - 100f, subTextPaint)

        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bitmap.recycle()
        return outFile
    }
}
