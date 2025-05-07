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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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
}