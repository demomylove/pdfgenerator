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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorUtil {

    private const val TAG = "PdfGeneratorUtil"

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

                val processedSuccessfully = inputStream.use { stream ->
                    var originalBitmap = BitmapFactory.decodeStream(stream)
                    if (originalBitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap from URI: $uri")
                        return@use false // Signal failure for this URI
                    }

                    // Apply image quality (compression) - this creates a new bitmap
                    val baos = ByteArrayOutputStream()
                    originalBitmap.compress(Bitmap.CompressFormat.JPEG, settings.imageQuality.compressionQuality, baos)
                    val compressedBitmapBytes = baos.toByteArray()
                    originalBitmap.recycle() // Recycle original after compression
                    originalBitmap = BitmapFactory.decodeByteArray(compressedBitmapBytes, 0, compressedBitmapBytes.size)

                    if (originalBitmap == null) {
                        Log.e(TAG, "Failed to decode bitmap after compression for URI: $uri")
                        return@use false // Signal failure for this URI
                    }

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
                    val bitmapRect = RectF(0f, 0f, originalBitmap.width.toFloat(), originalBitmap.height.toFloat())
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

                    canvas.drawBitmap(originalBitmap, matrix, paint)
                    pdfDocument.finishPage(page)
                    originalBitmap.recycle()
                    return@use true // Signal success for this URI
                }

                if (!processedSuccessfully) {
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
