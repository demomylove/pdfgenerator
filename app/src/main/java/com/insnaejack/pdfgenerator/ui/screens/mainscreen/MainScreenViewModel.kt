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
/**
 * ViewModel for the [MainScreen].
 * Manages the state related to image selection, PDF creation, permissions,
 * billing status, PDF settings, and image editing operations.
 *
 * @property application Provides application context for file operations and resource access.
 * @property billingClientWrapper Handles interactions with the Google Play Billing Library.
 */
class MainScreenViewModel @Inject constructor(
    private val application: Application, // Inject Application context
    val billingClientWrapper: BillingClientWrapper // Inject BillingClientWrapper
) : ViewModel() {

// --- Permissions ---
    /** The camera permission string (Manifest.permission.CAMERA). */
    val cameraPermission: String = Manifest.permission.CAMERA
/**
     * List of storage permissions required.
     * Uses [Manifest.permission.READ_MEDIA_IMAGES] for Android 13 (API 33) and above,
     * otherwise defaults to [Manifest.permission.READ_EXTERNAL_STORAGE].
     */
    val storagePermissions: List<String> = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

// --- UI Triggers & Image Selection State ---
// --- UI Triggers & Image Selection State ---
    /** StateFlow to signal the UI to launch the camera. */
    private val _triggerCameraLaunch = MutableStateFlow(false)
    val triggerCameraLaunch: StateFlow<Boolean> = _triggerCameraLaunch.asStateFlow()

/** StateFlow to signal the UI to launch the image gallery picker. */
    private val _triggerGalleryLaunch = MutableStateFlow(false)
    val triggerGalleryLaunch: StateFlow<Boolean> = _triggerGalleryLaunch.asStateFlow()

/** StateFlow holding the list of URIs for images currently selected by the user. */
    private val _selectedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImageUris: StateFlow<List<Uri>> = _selectedImageUris.asStateFlow()

/** Temporary storage for the URI of the image file created before launching the camera intent. */
    private var tempCameraImageUri: Uri? = null

// --- PDF Creation State ---
    /** StateFlow holding the result of the last PDF creation attempt ([PdfGeneratorUtil.PdfCreationResult]). Null if no attempt made or status consumed. */
    private val _pdfCreationStatus = MutableStateFlow<PdfGeneratorUtil.PdfCreationResult?>(null)
    val pdfCreationStatus: StateFlow<PdfGeneratorUtil.PdfCreationResult?> = _pdfCreationStatus.asStateFlow()

/** StateFlow indicating whether the PDF creation process is currently active. */
    private val _isCreatingPdf = MutableStateFlow(false)
    val isCreatingPdf: StateFlow<Boolean> = _isCreatingPdf.asStateFlow()

    // PDF Settings
/** StateFlow holding the current [PdfSettings] for PDF generation. Defaults to initial settings. */
    private val _pdfSettings = MutableStateFlow(PdfSettings()) // Default settings
    val pdfSettings: StateFlow<PdfSettings> = _pdfSettings.asStateFlow()

    // Billing related states
/** StateFlow exposing the map of available product details ([ProductDetailsWrapper]) from the billing client. */
    val premiumProductDetails: StateFlow<Map<String, ProductDetailsWrapper>> = billingClientWrapper.productDetailsMap
/** StateFlow indicating whether the current user is a premium subscriber. Sourced from the billing client. */
    val isPremiumUser: StateFlow<Boolean> = billingClientWrapper.isPremiumUser

    // Example: Combine premium status with another feature flag
/** 
     * StateFlow indicating if premium features are accessible. 
     * Currently a direct reflection of [isPremiumUser], but can be combined with other flags.
     */
    val canAccessPremiumFeatures: StateFlow<Boolean> = isPremiumUser // Simplified for now
        // .combine(anotherFeatureFlag) { isPremium, otherFlag -> isPremium && otherFlag }
       // .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

   // --- Image Editing State ---
/** StateFlow holding the URI of the image currently selected for editing (crop/rotate or adjust). Null if no image is selected for editing. */
   private val _imageToEditUri = MutableStateFlow<Uri?>(null)
   val imageToEditUri: StateFlow<Uri?> = _imageToEditUri.asStateFlow()

   // Separate state for brightness/contrast dialog trigger if needed
/** StateFlow indicating whether the brightness/contrast adjustment dialog should be shown. */
   private val _showBrightnessContrastDialog = MutableStateFlow(false)
   val showBrightnessContrastDialog: StateFlow<Boolean> = _showBrightnessContrastDialog.asStateFlow()

/** StateFlow indicating whether an image editing operation (e.g., brightness/contrast) is currently in progress. */
   private val _isApplyingEdit = MutableStateFlow(false)
   val isApplyingEdit: StateFlow<Boolean> = _isApplyingEdit.asStateFlow()

/** StateFlow holding an error message string if an image editing operation failed. Null otherwise. */
   private val _editError = MutableStateFlow<String?>(null)
   val editError: StateFlow<String?> = _editError.asStateFlow()


/**
    * Initializes the ViewModel by querying existing purchases and available product details
    * from the [billingClientWrapper] to ensure the premium status and product info are current.
    */
   init {
        // Query purchases when ViewModel is created to ensure premium status is up-to-date
        billingClientWrapper.queryPurchasesAsync()
        billingClientWrapper.queryProductDetails() // Ensure product details are fetched
    }

/**
     * Called when the "Take Photo" button is clicked.
     * Sets [_triggerCameraLaunch] to true if camera permission is granted,
     * otherwise, logs a message (permission request should be handled by UI).
     * @param hasCameraPermission True if camera permission is already granted.
     */
    fun onTakePhotoClicked(hasCameraPermission: Boolean) {
        if (hasCameraPermission) {
            _triggerCameraLaunch.value = true
        } else {
            println("Camera permission not granted. Request from UI.")
        }
    }

/**
     * Called when the "Select from Gallery" button is clicked.
     * Sets [_triggerGalleryLaunch] to true if storage permission is granted,
     * otherwise, logs a message (permission request should be handled by UI).
     * @param hasStoragePermission True if storage permission is already granted.
     */
    fun onSelectFromGalleryClicked(hasStoragePermission: Boolean) {
        if (hasStoragePermission) {
            _triggerGalleryLaunch.value = true
        } else {
            println("Storage permission not granted. Request from UI.")
        }
    }

/**
     * Resets the [_triggerCameraLaunch] state to false.
     * Called by the UI after the camera launch has been initiated.
     */
    fun onCameraLaunchTriggered() {
        _triggerCameraLaunch.value = false
    }

/**
     * Resets the [_triggerGalleryLaunch] state to false.
     * Called by the UI after the gallery launch has been initiated.
     */
    fun onGalleryLaunchTriggered() {
        _triggerGalleryLaunch.value = false
    }

/**
     * Adds the URI of an image captured by the camera to the list of selected images.
     * Also clears the [tempCameraImageUri].
     * @param uri The URI of the captured image. Can be null if capture failed.
     */
    fun addCapturedImageUri(uri: Uri?) {
        uri?.let {
            _selectedImageUris.value = _selectedImageUris.value + it
        }
        tempCameraImageUri = null
    }

/**
     * Adds a single image URI (typically from gallery single selection) to the list of selected images.
     * @param uri The URI of the selected image. Can be null.
     */
    fun addSelectedImageUri(uri: Uri?) {
        uri?.let {
            _selectedImageUris.value = _selectedImageUris.value + it
        }
    }

/**
     * Adds a list of image URIs (typically from gallery multiple selection) to the list of selected images.
     * @param uris The list of URIs of the selected images.
     */
    fun addSelectedImageUris(uris: List<Uri>) {
        _selectedImageUris.value = _selectedImageUris.value + uris
    }

/**
     * Removes a specific image URI from the list of selected images.
     * @param uri The URI of the image to remove.
     */
    fun removeImageUri(uri: Uri) {
        _selectedImageUris.value = _selectedImageUris.value - uri
    }

/**
     * Clears the list of selected image URIs and resets the PDF creation status.
     */
    fun clearSelectedImages() {
        _selectedImageUris.value = emptyList()
        _pdfCreationStatus.value = null // Clear previous status
    }

/**
     * Sets the temporary URI for an image file that is expected to be populated by the camera.
     * This URI is typically generated before launching the camera intent.
     * @param uri The [Uri] of the pre-created image file.
     */
    fun setTempCameraImageUri(uri: Uri) {
        tempCameraImageUri = uri
    }

/**
     * Retrieves the temporary URI stored for the camera image.
     * This is needed by the camera launcher to know where to save the image.
     * @return The stored temporary camera image [Uri], or null if none is set.
     */
    fun getTempCameraImageUri(): Uri? {
       return tempCameraImageUri
   }

   // --- Image Editing Actions ---

/**
    * Sets the [_imageToEditUri] to trigger the image editing process (e.g., launching uCrop).
    * Ensures the brightness/contrast dialog state is reset.
    * @param uri The [Uri] of the image to be edited.
    */
   fun onEditImageClicked(uri: Uri) {
       _imageToEditUri.value = uri
       _showBrightnessContrastDialog.value = false // Ensure only one edit type is triggered at a time
   }

/**
    * Sets the [_imageToEditUri] and triggers the [_showBrightnessContrastDialog] state
    * to show the brightness/contrast adjustment dialog for the specified image.
    * @param uri The [Uri] of the image to be adjusted.
    */
   fun onAdjustImageClicked(uri: Uri) {
       _imageToEditUri.value = uri
       _showBrightnessContrastDialog.value = true
   }

/**
    * Resets the [_imageToEditUri] state to null.
    * Called by the UI after the image editor (e.g., uCrop activity) has been launched,
    * indicating the trigger is no longer needed.
    */
   fun onImageEditLaunched() {
       // Reset after the UI has reacted and launched the editor (e.g., uCrop activity)
       _imageToEditUri.value = null
   }

/**
     * Resets the [_imageToEditUri] and [_showBrightnessContrastDialog] states.
     * Called when the brightness/contrast dialog is dismissed (either by applying changes or cancelling).
     */
    fun onBrightnessContrastDialogDismissed() {
        _imageToEditUri.value = null
        _showBrightnessContrastDialog.value = false
    }

/**
    * Resets the [_editError] state to null.
    * Called by the UI after an image editing error message has been shown to the user.
    */
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
/**
     * Initiates the PDF creation process using the currently selected image URIs and settings.
     * Updates [_isCreatingPdf] and [_pdfCreationStatus] based on the outcome.
     * Runs the PDF generation off the main thread using [viewModelScope] and [Dispatchers.IO].
     */
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

/**
     * Resets the [_pdfCreationStatus] to null.
     * Called by the UI after the status message (success or failure) has been shown to the user.
     */
    fun consumePdfCreationStatus() {
        _pdfCreationStatus.value = null
    }

    // --- PDF Settings Methods ---
/**
     * Updates the current [PdfSettings] used for PDF generation.
     * This action is restricted to premium users.
     * @param newSettings The new [PdfSettings] object to apply.
     */
    fun updatePdfSettings(newSettings: PdfSettings) {
        if (isPremiumUser.value) { // Only allow premium users to change settings
            _pdfSettings.value = newSettings
        }
    }

    // --- Billing Methods ---
/**
     * Manually triggers a query for the user's current purchases via the [billingClientWrapper].
     * This updates the [isPremiumUser] state.
     */
    fun refreshPremiumStatus() {
        billingClientWrapper.queryPurchasesAsync()
    }

/**
     * Retrieves the [ProductDetailsWrapper] for a specific product ID from the cached map.
     * @param productId The ID of the product to retrieve details for.
     * @return The [ProductDetailsWrapper] if found, otherwise null.
     */
    fun getProductDetails(productId: String): ProductDetailsWrapper? {
        return premiumProductDetails.value[productId]
    }

    // Call this when the ViewModel is cleared
/**
     * Called when the ViewModel is no longer used and will be destroyed.
     * Used for cleanup tasks. Currently, it only calls the superclass implementation.
     * Billing client connection cleanup might be handled at the Application level if it's a singleton.
     */
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
/**
     * Asynchronously applies brightness and contrast adjustments to an image specified by [sourceUri].
     *
     * This function performs the following steps:
     * 1. Decodes the [Bitmap] from the [sourceUri].
     * 2. Creates a [ColorMatrix] based on the provided [brightness] and [contrast] values.
     *    - Brightness: 0 means no change. Positive values increase brightness, negative values decrease it.
     *      The input [brightness] (typically -1 to 1) is scaled by 255.
     *    - Contrast: 1 means no change. Values > 1 increase contrast, values < 1 decrease it.
     * 3. Applies the [ColorMatrix] to the bitmap using a [Canvas] and [Paint].
     * 4. Saves the edited bitmap to a new temporary file in the app's cache directory.
     *    The filename includes a timestamp and the original filename (if available).
     *    The image is saved as JPEG with 95% quality.
     * 5. Returns the [Uri] of the newly created edited image file.
     *
     * This operation is performed on the [Dispatchers.IO] coroutine dispatcher to avoid blocking the main thread.
     * Input and output streams are managed and closed in a finally block. Bitmap recycling is commented out
     * as it's generally handled by the system when the Uri is used, but can be enabled if memory issues arise.
     *
     * @param sourceUri The [Uri] of the image to be adjusted.
     * @param brightness The brightness adjustment factor.
     * @param contrast The contrast adjustment factor.
     * @return The [Uri] of the new file containing the adjusted image.
     * @throws IllegalStateException if the input stream cannot be opened/reopened or if the bitmap cannot be decoded.
     * @throws Exception if any other part of the image processing or file saving fails.
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