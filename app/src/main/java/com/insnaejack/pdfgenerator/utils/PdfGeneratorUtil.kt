package com.insnaejack.pdfgenerator.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.insnaejack.pdfgenerator.model.PdfSettings
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorUtil {

    private const val TAG = "PdfGeneratorUtil"

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        Log.d(TAG, "Calculated inSampleSize: $inSampleSize for reqWidth=$reqWidth, reqHeight=$reqHeight (original: ${width}x$height)")
        return inSampleSize
    }

    data class PdfCreationResult(
        val success: Boolean,
        val filePath: String? = null,
        val errorMessage: String? = null,
    )

    fun createPdf(
        context: Context,
        imageUris: List<Uri>,
        settings: PdfSettings,
        fileNamePrefix: String = "GeneratedPDF",
    ): PdfCreationResult {
        if (imageUris.isEmpty()) {
            return PdfCreationResult(false, errorMessage = "No images provided.")
        }

        val pdfDocument = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for ((index, uri) in imageUris.withIndex()) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "Could not open InputStream for URI: $uri")
                    continue // Skips to the next URI in the for-loop
                }

                // --- START: Optimized Bitmap Loading ---
                // 1. Decode bounds to get dimensions without loading full image
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                var boundsStream = context.contentResolver.openInputStream(uri)
                if (boundsStream == null) {
                    Log.e(TAG, "Could not open InputStream for bounds decoding: $uri")
                    continue // Skip this image
                }
                boundsStream.use { BitmapFactory.decodeStream(it, null, options) }

                // Check if bounds were decoded successfully
                if (options.outWidth <= 0 || options.outHeight <= 0) {
                    Log.e(TAG, "Failed to decode image bounds for URI: $uri")
                    continue // Skip this image
                }

                // 2. Calculate inSampleSize based on target page dimensions (content area)
                // Use contentWidth/Height as target dimensions for scaling
                options.inSampleSize = calculateInSampleSize(options, settings.contentWidth, settings.contentHeight)

                // 3. Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false
                var scaledBitmap: Bitmap? = null
                var decodeStream = context.contentResolver.openInputStream(uri)
                if (decodeStream == null) {
                    Log.e(TAG, "Could not re-open InputStream for final decoding: $uri")
                    continue // Skip this image
                }
                decodeStream.use { scaledBitmap = BitmapFactory.decodeStream(it, null, options) }

                if (scaledBitmap == null) {
                    Log.e(TAG, "Failed to decode scaled bitmap from URI: $uri (inSampleSize: ${options.inSampleSize})")
                    continue // Skip this image
                }
                // --- END: Optimized Bitmap Loading ---

                val processedSuccessfully = try { // Wrap page creation in try-finally for bitmap recycling
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        settings.pageDisplayWidth,
                        settings.pageDisplayHeight,
                        index + 1,
                    ).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    val contentWidth = settings.contentWidth.toFloat()
                    val contentHeight = settings.contentHeight.toFloat()

                    // Calculate scaling to fit within content area while maintaining aspect ratio
                    // Use scaledBitmap dimensions here
                    val bitmapRect = RectF(0f, 0f, scaledBitmap!!.width.toFloat(), scaledBitmap!!.height.toFloat())
                    val contentRect = RectF(0f, 0f, contentWidth, contentHeight)
                    val matrix = Matrix()
                    matrix.setRectToRect(bitmapRect, contentRect, Matrix.ScaleToFit.CENTER)

                    // Calculate final draw position within margins
                    val finalRect = RectF()
                    matrix.mapRect(finalRect, bitmapRect) // Get the bounds of the scaled image

                    val drawLeft = settings.marginLeft + (contentWidth - finalRect.width()) / 2f
                    val drawTop = settings.marginTop + (contentHeight - finalRect.height()) / 2f

                    // Apply the calculated left and top offsets to the matrix
                    matrix.postTranslate(drawLeft, drawTop)

                    canvas.drawBitmap(scaledBitmap!!, matrix, paint) // Use scaledBitmap

                    // --- START: Draw Watermark ---
                    if (settings.watermarkEnabled && !settings.watermarkText.isNullOrBlank()) {
                        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = settings.watermarkTextColor
                            textSize = settings.watermarkTextSize
                            textAlign = Paint.Align.CENTER
                            // Consider adding typeface if needed: typeface = Typeface.create(...)
                        }
                        // Calculate center of the page
                        val centerX = pageInfo.pageWidth / 2f
                        val centerY = pageInfo.pageHeight / 2f

                        canvas.save() // Save current canvas state
                        canvas.translate(centerX, centerY) // Move origin to center
                        canvas.rotate(settings.watermarkRotation) // Rotate canvas
                        // Draw text centered at the new origin (0,0)
                        // Adjust Y slightly for better visual centering based on text height
                        val textBounds = android.graphics.Rect()
                        watermarkPaint.getTextBounds(settings.watermarkText, 0, settings.watermarkText.length, textBounds)
                        canvas.drawText(settings.watermarkText, 0f, textBounds.height() / 2f, watermarkPaint)
                        canvas.restore() // Restore canvas state
                    }
                    // --- END: Draw Watermark ---

                    pdfDocument.finishPage(page)
                    true // Signal success for this page
                } finally {
                    scaledBitmap?.recycle() // Recycle the scaled bitmap in a finally block
                }

                if (!processedSuccessfully) {
                    Log.w(TAG, "Processing failed for URI: $uri")
                    continue // Skips to the next URI in the for-loop if processing failed
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image URI: $uri", e)
                continue
            }
        }

        if (pdfDocument.pages.isEmpty()) {
            pdfDocument.close()
            return PdfCreationResult(false, errorMessage = "No images could be processed to create PDF.")
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${fileNamePrefix}_$timeStamp.pdf"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

        if (storageDir == null) {
            pdfDocument.close()
            return PdfCreationResult(false, errorMessage = "Failed to access app's document directory.")
        }
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val pdfFile = File(storageDir, fileName)

        return try {
            FileOutputStream(pdfFile).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()
            Log.i(TAG, "PDF created successfully at ${pdfFile.absolutePath}")
            PdfCreationResult(true, filePath = pdfFile.absolutePath)
        } catch (e: IOException) {
            Log.e(TAG, "Error writing PDF to file", e)
            pdfDocument.close()
            PdfCreationResult(false, errorMessage = "Error saving PDF: ${e.localizedMessage}")
        }
    }
}
