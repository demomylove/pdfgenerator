package com.insnaejack.pdfgenerator.ui.screens.mainscreen

import android.Manifest
import android.app.Application // Required for context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insnaejack.pdfgenerator.billing.BillingClientWrapper
import com.insnaejack.pdfgenerator.billing.ProductDetailsWrapper
import com.insnaejack.pdfgenerator.model.PdfSettings // Import PdfSettings
import com.insnaejack.pdfgenerator.utils.PdfGeneratorUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.graphics.* // Needed for Bitmap, Canvas, Paint, ColorMatrix, ColorMatrixColorFilter
import androidx.core.net.toUri // For converting File to Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainScreenViewModel @Inject constructor(
    private val application: Application, // Inject Application context
    val billingClientWrapper: BillingClientWrapper // Inject BillingClientWrapper
) : ViewModel() {

    val cameraPermission: String = Manifest.permission.CAMERA
    val storagePermissions: List<String> = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private val _triggerCameraLaunch = MutableStateFlow(false)
    val triggerCameraLaunch: StateFlow<Boolean> = _triggerCameraLaunch.asStateFlow()

    private val _triggerGalleryLaunch = MutableStateFlow(false)
    val triggerGalleryLaunch: StateFlow<Boolean> = _triggerGalleryLaunch.asStateFlow()

    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

    private var tempCameraImageUri: Uri? = null

    private val _pdfCreationStatus = MutableStateFlow<PdfGeneratorUtil.PdfCreationResult?>(null)
    val pdfCreationStatus: StateFlow<PdfGeneratorUtil.PdfCreationResult?> = _pdfCreationStatus.asStateFlow()

    private val _isCreatingPdf = MutableStateFlow(false)
    val isCreatingPdf: StateFlow<Boolean> = _isCreatingPdf.asStateFlow()

    // PDF Settings
    private val _pdfSettings = MutableStateFlow(PdfSettings()) // Default settings
    val pdfSettings: StateFlow<PdfSettings> = _pdfSettings.asStateFlow()

    // Billing related states
    val premiumProductDetails: StateFlow<Map<String, ProductDetailsWrapper>> = billingClientWrapper.productDetailsMap
    val isPremiumUser: StateFlow<Boolean> = billingClientWrapper.isPremiumUser

    // Example: Combine premium status with another feature flag
    val canAccessPremiumFeatures: StateFlow<Boolean> = isPremiumUser // Simplified for now
        // .combine(anotherFeatureFlag) { isPremium, otherFlag -> isPremium && otherFlag }
       // .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

   // --- Image Editing State ---
   private val _imageToEditUri = MutableStateFlow<Uri?>(null)
   val imageToEditUri: StateFlow<Uri?> = _imageToEditUri.asStateFlow()

   // Separate state for brightness/contrast dialog trigger if needed
   private val _showBrightnessContrastDialog = MutableStateFlow(false)
   val showBrightnessContrastDialog: StateFlow<Boolean> = _showBrightnessContrastDialog.asStateFlow()

   private val _isApplyingEdit = MutableStateFlow(false)
   val isApplyingEdit: StateFlow<Boolean> = _isApplyingEdit.asStateFlow()

   private val _editError = MutableStateFlow<String?>(null)
   val editError: StateFlow<String?> = _editError.asStateFlow()


   init {
        // Query purchases when ViewModel is created to ensure premium status is up-to-date
        billingClientWrapper.queryPurchasesAsync()
        billingClientWrapper.queryProductDetails() // Ensure product details are fetched
    }

    fun onTakePhotoClicked(hasCameraPermission: Boolean) {
        if (hasCameraPermission) {
            _triggerCameraLaunch.value = true
        } else {
            println("Camera permission not granted. Request from UI.")
        }
    }

    fun onSelectFromGalleryClicked(hasStoragePermission: Boolean) {
        if (hasStoragePermission) {
            _triggerGalleryLaunch.value = true
        } else {
            println("Storage permission not granted. Request from UI.")
        }
    }

    fun onCameraLaunchTriggered() {
        _triggerCameraLaunch.value = false
    }

    fun onGalleryLaunchTriggered() {
        _triggerGalleryLaunch.value = false
    }

    fun addCapturedImageUri(uri: Uri?) {
        uri?.let {
            _selectedImageUris.value = _selectedImageUris.value + it
        }
        tempCameraImageUri = null
    }

    fun addSelectedImageUri(uri: Uri?) {
        uri?.let {
            _selectedImageUris.value = _selectedImageUris.value + it
        }
    }

    fun addSelectedImageUris(uris: List<Uri>) {
        _selectedImageUris.value = _selectedImageUris.value + uris
    }

    fun removeImageUri(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value - uri
    }

    fun clearSelectedImages() {
        _selectedImageUris.value = emptyList()
        _pdfCreationStatus.value = null // Clear previous status
    }

    fun setTempCameraImageUri(uri: Uri) {
        tempCameraImageUri = uri
    }

    fun getTempCameraImageUri(): Uri? {
       return tempCameraImageUri
   }

   // --- Image Editing Actions ---

   fun onEditImageClicked(uri: Uri) {
       _imageToEditUri.value = uri
       _showBrightnessContrastDialog.value = false // Ensure only one edit type is triggered at a time
   }

   fun onAdjustImageClicked(uri: Uri) {
       _imageToEditUri.value = uri
       _showBrightnessContrastDialog.value = true
   }

   fun onImageEditLaunched() {
       // Reset after the UI has reacted and launched the editor (e.g., uCrop activity)
       _imageToEditUri.value = null
   }

    fun onBrightnessContrastDialogDismissed() {
        _imageToEditUri.value = null
        _showBrightnessContrastDialog.value = false
    }

   fun consumeEditError() {
       _editError.value = null
   }

   /**
    * Replaces the original image URI with the newly edited one in the list.
    * Typically called after uCrop finishes or brightness/contrast is applied.
    */
   fun updateEditedImage(originalUri: Uri, editedUri: Uri) {
       _selectedImageUris.update { currentList: List<Uri> -> // Explicitly type lambda parameter
           val index = currentList.indexOf(originalUri)
           if (index != -1) {
               currentList.toMutableList().apply { this[index] = editedUri } // Use this[index] for MutableList.set
           } else {
               currentList // Return unchanged list if original URI not found
           }
       }
       // Clean up the trigger state if the edit originated from brightness/contrast dialog
       if (_showBrightnessContrastDialog.value && _imageToEditUri.value == originalUri) {
            onBrightnessContrastDialogDismissed()
       }
   }

   /**
    * Applies brightness and contrast adjustments to the image specified by targetUri.
    * Saves the result to a new file and calls updateEditedImage.
    * Brightness: 0 = no change, >0 = brighter, <0 = darker. Typically -1 to 1 range makes sense.
    * Contrast: 1 = no change, >1 = higher contrast, <1 = lower contrast. Typically 0 to 2 range.
    */
   fun applyBrightnessContrast(targetUri: Uri, brightness: Float, contrast: Float) {
       viewModelScope.launch {
           _isApplyingEdit.value = true
           _editError.value = null
           try {
               val editedUri = applyColorMatrixAdjustments(targetUri, brightness, contrast)
               updateEditedImage(targetUri, editedUri)
           } catch (e: Exception) {
               _editError.value = "Failed to apply adjustments: ${e.message}"
               e.printStackTrace()
                onBrightnessContrastDialogDismissed() // Close dialog on error
           } finally {
               _isApplyingEdit.value = false
           }
       }
   }

   // --- PDF Creation ---
   fun createPdfFromSelectedImages() {
        if (_selectedImageUris.value.isEmpty()) {
            _pdfCreationStatus.value = PdfGeneratorUtil.PdfCreationResult(false, errorMessage = "No images selected.")
            return
        }
        viewModelScope.launch {
            _isCreatingPdf.value = true
            _pdfCreationStatus.value = null // Clear previous status
            // Run PDF creation in a background thread if it's very intensive,
            // though PdfDocument API itself is reasonably efficient.
            // For very large numbers of images or complex processing, consider Dispatchers.IO
            val currentSettings = if (isPremiumUser.value) {
                _pdfSettings.value // Use user-defined settings if premium
            } else {
                PdfSettings() // Use default settings if not premium
            }
            val result = PdfGeneratorUtil.createPdf(application.applicationContext, _selectedImageUris.value, currentSettings)
            _pdfCreationStatus.value = result
            if (result.success) {
                // Optionally clear images after successful PDF creation
                // clearSelectedImages()
            }
            _isCreatingPdf.value = false
        }
    }

    fun consumePdfCreationStatus() {
        _pdfCreationStatus.value = null
    }

    // --- PDF Settings Methods ---
    fun updatePdfSettings(newSettings: PdfSettings) {
        if (isPremiumUser.value) { // Only allow premium users to change settings
            _pdfSettings.value = newSettings
        }
    }

    // --- Billing Methods ---
    fun refreshPremiumStatus() {
        billingClientWrapper.queryPurchasesAsync()
    }

    fun getProductDetails(productId: String): ProductDetailsWrapper? {
        return premiumProductDetails.value[productId]
    }

    // Call this when the ViewModel is cleared
    override fun onCleared() {
        // billingClientWrapper.endConnection() // Singleton, might be managed by Application lifecycle
        super.onCleared()
    }
    // --- Private Helper for Image Adjustments ---

    /**
     * Loads bitmap, applies brightness/contrast using ColorMatrix, saves to a new file.
     * Runs on Dispatchers.IO.
     * @return The Uri of the newly created edited image file.
     * @throws Exception if any step fails.
     */
    private suspend fun applyColorMatrixAdjustments(sourceUri: Uri, brightness: Float, contrast: Float): Uri = withContext(Dispatchers.IO) {
        val context = application.applicationContext // Now 'application' is accessible
        var inputStream: java.io.InputStream? = null
        var outputStream: FileOutputStream? = null
        var originalBitmap: Bitmap? = null
        var editedBitmap: Bitmap? = null
        val tempOutputFile: File

        try {
            // 1. Decode Bitmap from Uri
            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Could not open input stream for URI: $sourceUri")

            // Use BitmapFactory.Options to handle potential large images and check bounds
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close() // Close stream after getting bounds

            // Calculate inSampleSize if needed (optional, for memory optimization)
            // options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false // Now decode the actual bitmap

            inputStream = context.contentResolver.openInputStream(sourceUri) // Reopen stream
                 ?: throw IllegalStateException("Could not reopen input stream for URI: $sourceUri")
            originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                ?: throw IllegalStateException("Could not decode bitmap from URI: $sourceUri")

            // 2. Create ColorMatrix for brightness and contrast
            val brightnessValue = brightness * 255f
            val scale = contrast
            val translate = (-0.5f * scale + 0.5f) * 255f + brightnessValue
            val colorMatrix = ColorMatrix(floatArrayOf(
                scale, 0f,    0f,    0f, translate, // Red
                0f,    scale, 0f,    0f, translate, // Green
                0f,    0f,    scale, 0f, translate, // Blue
                0f,    0f,    0f,    1f, 0f         // Alpha
            ))

            // 3. Apply ColorMatrix using Paint and Canvas
            // Ensure the edited bitmap is mutable if the original wasn't
            editedBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(editedBitmap)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
                isAntiAlias = true
            }
            // Draw the original bitmap onto the mutable edited bitmap with the paint filter
            // We draw the original onto the *copy* we made mutable.
            canvas.drawBitmap(originalBitmap, 0f, 0f, paint)


            // 4. Save edited Bitmap to a temporary file
            val timestamp = System.currentTimeMillis()
            // Use JPEG for potentially smaller file size, or PNG for lossless
            val extension = "jpg"
            val mimeType = "image/jpeg"
            val quality = 95 // For JPEG
            tempOutputFile = File(context.cacheDir, "edited_${timestamp}_${sourceUri.lastPathSegment?.substringBeforeLast('.') ?: "image"}.$extension")

            outputStream = FileOutputStream(tempOutputFile)
            val compressFormat = if (extension == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            editedBitmap.compress(compressFormat, quality, outputStream)
            outputStream.flush()

            // 5. Return Uri of the new file
            tempOutputFile.toUri()

        } finally {
            // Clean up resources
            inputStream?.close()
            outputStream?.close()
            // Recycle original bitmap only if it's safe (not used elsewhere)
            // originalBitmap?.recycle()
            // editedBitmap is implicitly handled by the Uri pointing to the saved file,
            // but explicit recycle might be needed if keeping many bitmaps in memory.
            // editedBitmap?.recycle()
        }
    }
}