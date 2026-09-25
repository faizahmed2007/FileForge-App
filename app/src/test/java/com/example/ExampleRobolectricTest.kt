package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ImageCompressionOptions
import com.example.engine.ImageProcessingUtils
import com.example.engine.SupportedImageFormat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FileForge Offline", appName)
  }

  @Test
  fun `verify supported image formats resolution`() {
    assertEquals(SupportedImageFormat.JPEG, SupportedImageFormat.fromExtension("jpg"))
    assertEquals(SupportedImageFormat.JPEG, SupportedImageFormat.fromExtension("jpeg"))
    assertEquals(SupportedImageFormat.PNG, SupportedImageFormat.fromExtension("png"))
    assertEquals(SupportedImageFormat.WEBP, SupportedImageFormat.fromExtension("webp"))

    assertEquals(SupportedImageFormat.PNG, SupportedImageFormat.fromString("PNG"))
    assertEquals(SupportedImageFormat.JPEG, SupportedImageFormat.fromString("JPG"))
    assertEquals(SupportedImageFormat.WEBP, SupportedImageFormat.fromString("WEBP"))
  }

  @Test
  fun `test image format conversion using Android Bitmap API`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempDir = context.cacheDir
    val testInputFile = File(tempDir, "test_input.png")

    // Create a simple test PNG bitmap
    val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
    FileOutputStream(testInputFile).use { fos ->
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }

    // Test conversion from PNG to JPEG
    val result = ImageProcessingUtils.convertImageFormat(
      context = context,
      inputFile = testInputFile,
      targetFormat = SupportedImageFormat.JPEG,
      quality = 85
    )

    assertTrue("Conversion to JPEG should succeed", result.success)
    assertNotNull("Output file should not be null", result.outputFile)
    assertTrue("Output file must exist", result.outputFile!!.exists())
    assertEquals(SupportedImageFormat.JPEG, result.targetFormat)
    assertEquals(200, result.width)
    assertEquals(200, result.height)

    // Cleanup
    testInputFile.delete()
    result.outputFile?.delete()
    bitmap.recycle()
  }

  @Test
  fun `test local image compression maintaining quality`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempDir = context.cacheDir
    val testInputFile = File(tempDir, "test_large.jpg")

    // Create a 800x800 bitmap
    val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
    FileOutputStream(testInputFile).use { fos ->
      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
    }

    val options = ImageCompressionOptions(
      quality = 70,
      maxDimension = 400,
      outputFormat = SupportedImageFormat.JPEG
    )

    val result = ImageProcessingUtils.compressImageMaintainingQuality(
      context = context,
      inputFile = testInputFile,
      options = options
    )

    assertTrue("Compression should succeed", result.success)
    assertNotNull("Compressed output file must not be null", result.outputFile)
    assertTrue("Compressed file must exist", result.outputFile!!.exists())
    assertEquals(800, result.originalWidth)
    assertEquals(800, result.originalHeight)
    assertEquals(400, result.compressedWidth)
    assertEquals(400, result.compressedHeight)

    // Cleanup
    testInputFile.delete()
    result.outputFile?.delete()
    bitmap.recycle()
  }
}
