package com.insnaejack.pdfgenerator.ui.screens.mainscreen

import android.Manifest
import android.graphics.Bitmap
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.insnaejack.pdfgenerator.R
import com.insnaejack.pdfgenerator.billing.ProductIds
import com.insnaejack.pdfgenerator.model.PdfSettings
import com.insnaejack.pdfgenerator.ui.screens.settings.PdfSettingsDialog
import com.insnaejack.pdfgenerator.ui.theme.PdfGeneratorTheme
import com.google.accompanist.permissions.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.yalantis.ucrop.UCrop // Import uCrop
import com.yalantis.ucrop.UCropActivity // Needed for allowedGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.CropRotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat // For UCrop options colors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.CircularProgressIndicator // For loading in dialog

/**
 * Creates a temporary image file in the external cache directory.
 * Used for storing the image captured by the camera before processing.
 * @return A File object representing the created image file.
 */
fun Context.createImageFile(): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_" + timeStamp + "_"
    return File.createTempFile(imageFileName, ".jpg", externalCacheDir)
}



@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainScreenViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
/**
 * The main screen composable for the PDF Generator application.
 * Handles user interactions for selecting images (camera, gallery, Google Drive),
 * managing permissions, displaying selected images, initiating PDF creation,
 * handling premium upgrades, and displaying ads for non-premium users.
 *
 * @param navController The NavController for navigating between screens.
 * @param viewModel The [MainScreenViewModel] instance providing data and handling logic.
 */

    val cameraPermissionState = rememberPermissionState(permission = viewModel.cameraPermission)
    val storagePermissionsState = rememberMultiplePermissionsState(permissions = viewModel.storagePermissions)

    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showStorageRationaleDialog by remember { mutableStateOf(false) }
    var showPdfSettingsDialog by remember { mutableStateOf(false) }

    val triggerCameraLaunch by viewModel.triggerCameraLaunch.collectAsState()
    val triggerGalleryLaunch by viewModel.triggerGalleryLaunch.collectAsState()
    val selectedImageUris by viewModel.selectedImageUris.collectAsState()
    val pdfCreationStatus by viewModel.pdfCreationStatus.collectAsState()
    val isCreatingPdf by viewModel.isCreatingPdf.collectAsState()

    val premiumProductDetailsMap by viewModel.premiumProductDetails.collectAsState()
/**
     * Observes purchase updates from the BillingClientWrapper.
     * Handles successful purchases, cancellations, and errors, providing user feedback via Toasts.
     * Refreshes premium status upon successful purchase.
     */
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val premiumProduct = premiumProductDetailsMap[ProductIds.PREMIUM_UPGRADE_ID]
val imageToEditUriState by viewModel.imageToEditUri.collectAsState()
    val showBrightnessContrastDialogState by viewModel.showBrightnessContrastDialog.collectAsState()
    val currentPdfSettings by viewModel.pdfSettings.collectAsState() // Ensure this is present

    LaunchedEffect(Unit) {
        viewModel.billingClientWrapper.purchaseUpdateEvent.collect { event ->
            event?.let { (billingResult, purchases) ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        purchases?.forEach { purchase ->
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                                purchase.products.contains(ProductIds.PREMIUM_UPGRADE_ID)) {
                                Toast.makeText(context, R.string.purchase_successful, Toast.LENGTH_LONG).show()
                                viewModel.refreshPremiumStatus()
                            }
                        }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
/**
     * ActivityResultLauncher for the camera intent (TakePicture contract).
     * Handles the result of taking a picture:
     * - If successful, adds the captured image URI (obtained from ViewModel) to the selected list.
     * - If unsuccessful (e.g., user cancelled), resets the temporary camera image URI in the ViewModel.
     */
                        Toast.makeText(context, "Purchase cancelled", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Purchase failed: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                    }
/**
     * ActivityResultLauncher for picking multiple visual media (PickMultipleVisualMedia contract).
     * Handles the result of selecting images from the photo picker (if available).
     * - If URIs are returned, adds them to the ViewModel's selected image list.
     */
                }
                viewModel.billingClientWrapper.consumePurchaseUpdateEvent()
            }
/**
     * ActivityResultLauncher for getting multiple contents (GetMultipleContents contract).
     * This is a fallback for when the photo picker (PickVisualMedia) is not available.
     * Handles the result of selecting images from the older storage access framework.
     * - If URIs are returned, adds them to the ViewModel's selected image list.
     */
        }
    }
/**
    * ActivityResultLauncher for the uCrop image cropping activity (StartActivityForResult contract).
    * Handles the result of the cropping operation:
    * - If successful (RESULT_OK), retrieves the cropped image URI and updates the corresponding
    *   image URI in the ViewModel using the original URI for mapping.
    * - If there's an error (UCrop.RESULT_ERROR), displays an error Toast.
    * - Resets the image editing trigger in the ViewModel regardless of the outcome.
    */

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                viewModel.addCapturedImageUri(viewModel.getTempCameraImageUri())
            } else {
                viewModel.setTempCameraImageUri(Uri.EMPTY)
            }
        }
    )

    val pickMultipleMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
/**
     * Launches the camera intent when the `triggerCameraLaunch` state in the ViewModel becomes true.
     * Creates a temporary file, gets its content URI via FileProvider, sets it in the ViewModel,
     * launches the `takePictureLauncher`, and resets the trigger state in the ViewModel.
     */
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addSelectedImageUris(uris)
            }
        }
/**
     * Launches the appropriate image picker (Photo Picker or GetContent) when the
     * `triggerGalleryLaunch` state in the ViewModel becomes true.
     * Checks if the modern Photo Picker is available and uses it if possible, otherwise falls back
     * to the older GetMultipleContents contract. Resets the trigger state in the ViewModel.
     */
    )
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri>? ->
            uris?.let { viewModel.addSelectedImageUris(it) }
       }
   )

   // Launcher for uCrop Activity
   val uCropLauncher = rememberLauncherForActivityResult(
       contract = ActivityResultContracts.StartActivityForResult()
   ) { result ->
/**
     * Observes the `pdfCreationStatus` from the ViewModel.
     * Displays a Toast message indicating success or failure of PDF creation.
     * If successful, clears the selected images.
     * Consumes the status event in the ViewModel to prevent re-triggering.
     */
       if (result.resultCode == Activity.RESULT_OK) {
           val resultUri = result.data?.let { UCrop.getOutput(it) }
           val originalUri = viewModel.imageToEditUri.value // Get the original URI that was being edited
           if (resultUri != null && originalUri != null) {
               viewModel.updateEditedImage(originalUri, resultUri)
           } else {
               Toast.makeText(context, "Failed to get cropped image.", Toast.LENGTH_SHORT).show()
           }
       } else if (result.resultCode == UCrop.RESULT_ERROR) {
           val cropError = result.data?.let { UCrop.getError(it) }
/**
     * Launches the uCrop activity when an image URI is set in `imageToEditUriState`
     * and the brightness/contrast dialog is not currently shown (`showBrightnessContrastDialogState`).
     * This effect triggers the primary image editing (cropping, rotation) flow.
     * Configures uCrop options (compression, controls, colors, gestures) and launches
     * the `uCropLauncher` with the prepared intent.
     * The ViewModel state (`imageToEditUri`) is reset within the `uCropLauncher`'s result handler.
     */
           Toast.makeText(context, "Image cropping error: ${cropError?.message}", Toast.LENGTH_LONG).show()
           cropError?.printStackTrace()
       }
       // Reset the trigger in ViewModel regardless of result
       viewModel.onImageEditLaunched()
   }


   LaunchedEffect(triggerCameraLaunch) {
        if (triggerCameraLaunch) {
            val photoFile = context.createImageFile()
            val photoURI = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
            viewModel.setTempCameraImageUri(photoURI)
            takePictureLauncher.launch(photoURI)
            viewModel.onCameraLaunchTriggered()
        }
    }

    LaunchedEffect(triggerGalleryLaunch) {
        Log.d("GalleryLaunchEffect", "triggerGalleryLaunch changed: $triggerGalleryLaunch")
        if (triggerGalleryLaunch) {
            Log.d("GalleryLaunchEffect", "Triggering gallery launch.")
            val photoPickerAvailable = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
            Log.d("GalleryLaunchEffect", "Is Photo Picker Available: $photoPickerAvailable")
            if (photoPickerAvailable) {
                Log.d("GalleryLaunchEffect", "Launching PickVisualMedia.")
                pickMultipleMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                Log.d("GalleryLaunchEffect", "Photo Picker not available. Launching GetMultipleContents.")
                getContentLauncher.launch("image/*")
            }
            viewModel.onGalleryLaunchTriggered()
/**
    * Main layout structure using Scaffold.
/**
             * Top app bar displaying the application title centrally.
             * Contains action buttons for clearing selection, upgrading, and accessing settings.
             */
    * Provides slots for top bar, bottom bar (for ads), and the main content area.
    */
            Log.d("GalleryLaunchEffect", "Called onGalleryLaunchTriggered to reset flag.")
// Action to clear all currently selected images.
                        // Visible only if images are selected and PDF creation is not in progress.
        }
    }

// Action to initiate the premium upgrade purchase flow.
                        // Visible only for non-premium users when premium product details are available.
    LaunchedEffect(pdfCreationStatus) {
        pdfCreationStatus?.let { result ->
            val message = if (result.success) {
                context.getString(R.string.pdf_created_successfully, result.filePath ?: "N/A")
            } else {
                context.getString(R.string.error_creating_pdf, result.errorMessage ?: "Unknown error")
            }
// Action to open the PDF Settings dialog.
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            if (result.success) {
                viewModel.clearSelectedImages()
            }
            viewModel.consumePdfCreationStatus()
       }
   }
/**
             * Bottom bar containing the banner ad.
             * Displayed only for non-premium users.
             */
/**
         * Main content area of the screen.
         * Contains the premium upgrade button (if applicable),
         * buttons for taking photos and selecting from gallery,
         * a button for importing from Google Drive (premium users),
         * a display area for selected images, and the "Create PDF" button.
         * Shows a progress indicator when PDF creation is in progress.
         * Shows a placeholder if no images are selected.
// Button to initiate premium purchase flow, shown prominently for non-premium users.
                // Displays the formatted price from the product details.
         */

   // Effect to launch uCrop when imageToEditUri is set and dialog is not shown
   LaunchedEffect(imageToEditUriState, showBrightnessContrastDialogState) {
       val uriToEdit = imageToEditUriState // Use the collected state
       val showDialog = showBrightnessContrastDialogState // Use the collected state
       if (uriToEdit != null && !showDialog) {
           // Create a destination URI for the cropped image
           val destinationFileName = "cropped_${System.currentTimeMillis()}.jpg"
           val destinationUri = Uri.fromFile(File(context.cacheDir, destinationFileName))

/**
             * Row containing the primary actions for adding images: Take Photo and Select from Gallery.
             */
           // Configure uCrop options
           val options = UCrop.Options().apply {
               setCompressionFormat(Bitmap.CompressFormat.JPEG)
/**
                     * Button to initiate taking a photo.
                     * Checks camera permission status:
                     * - If granted, triggers camera launch via ViewModel.
                     * - If rationale should be shown, displays the rationale dialog.
                     * - Otherwise, requests the camera permission directly.
                     * Disabled during PDF creation.
                     */
               setCompressionQuality(90) // Adjust quality as needed
               setHideBottomControls(false)
               setFreeStyleCropEnabled(true)
               // Optional: Customize toolbar color, etc.
                setToolbarColor(ContextCompat.getColor(context, R.color.purple_500)) // Example color
                setStatusBarColor(ContextCompat.getColor(context, R.color.purple_700)) // Example color
                setActiveControlsWidgetColor(ContextCompat.getColor(context, R.color.teal_200)) // Example color
                setToolbarWidgetColor(ContextCompat.getColor(context, R.color.white)) // Example color
/**
                     * Button to initiate selecting images from the gallery.
                     * Checks storage permission status:
                     * - If granted, triggers gallery launch via ViewModel.
                     * - If rationale should be shown for any storage permission, displays the rationale dialog.
                     * - Otherwise, requests storage permissions directly.
                     * Disabled during PDF creation.
                     */
                setRootViewBackgroundColor(ContextCompat.getColor(context, R.color.black)) // Example color

               // Allow specific gestures
               setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
           }

           // Build and launch uCrop Intent
           val uCropIntent = UCrop.of(uriToEdit, destinationUri)
               .withOptions(options)
               // Optional: Set aspect ratio
               // .withAspectRatio(1f, 1f)
               // Optional: Set max size
               // .withMaxResultSize(1000, 1000)
               .getIntent(context)

           uCropLauncher.launch(uCropIntent)
           // ViewModel state is reset in the uCropLauncher result handling
       }
   }
/**
                 * Button to navigate to the Google Drive screen for importing images.
                 * Visible only for premium users.
                 */


   Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    if (selectedImageUris.isNotEmpty() && !isCreatingPdf) {
                        IconButton(onClick = { viewModel.clearSelectedImages() }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear selected images")
                        }
                    }
// Display a loading indicator and text when PDF creation is in progress.
                    if (!isPremiumUser && premiumProduct != null) {
                        IconButton(onClick = {
                            activity?.let { act ->
                                viewModel.billingClientWrapper.launchPurchaseFlow(act, premiumProduct.productDetails)
                            }
                        }) {
                            Icon(Icons.Outlined.WorkspacePremium, contentDescription = stringResource(R.string.upgrade_to_premium))
                        }
// Display selected images horizontally in a LazyRow when PDF creation is not active.
                // Shows the count of selected images above the row.
                    }
                    // PDF Settings Icon
                    IconButton(onClick = { showPdfSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                }
            )
        },
        bottomBar = {
            if (!isPremiumUser) {
                AdvertView(modifier = Modifier.fillMaxWidth())
            }
/**
                 * Button to initiate the PDF creation process using the currently selected images.
                 * Enabled only when images are selected and PDF creation is not already in progress.
                 */
        }
    ) { paddingValues ->
        Column(
// Placeholder displayed in the center when no images are selected.
                // Uses weight modifier to occupy remaining vertical space.
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(horizontal = 16.dp) // Add horizontal padding for content
                .padding(top = 16.dp), // Add top padding for content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isPremiumUser && premiumProduct != null) {
                Button(onClick = {
                    activity?.let { act ->
                        viewModel.billingClientWrapper.launchPurchaseFlow(act, premiumProduct.productDetails)
                    }
/**
     * Dialog to explain why camera permission is needed.
     * Shown when `showCameraRationaleDialog` is true.
     * On confirmation, launches the camera permission request.
     * On dismissal, hides the dialog.
     */
                }) {
                    Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("${stringResource(R.string.upgrade_to_premium)} - ${premiumProduct.formattedPrice}")
                }
                Spacer(modifier = Modifier.height(16.dp))
/**
     * Dialog to explain why storage permission is needed.
     * Shown when `showStorageRationaleDialog` is true.
     * On confirmation, launches the storage permissions request.
     * On dismissal, hides the dialog.
     */
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
/**
     * Dialog for configuring PDF generation settings (e.g., page size, orientation, quality).
     * Shown when `showPdfSettingsDialog` is true.
     * Passes current settings and premium status to the dialog.
     * Updates settings in the ViewModel when changes are applied.
     * Shows a Toast message indicating update success or prompting for premium upgrade.
     */
                    onClick = {
                        when {
                            cameraPermissionState.status.isGranted -> viewModel.onTakePhotoClicked(true)
                            cameraPermissionState.status.shouldShowRationale -> showCameraRationaleDialog = true
                            else -> cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    enabled = !isCreatingPdf
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = stringResource(R.string.take_photo))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.take_photo))
                }
/**
     * Dialog for adjusting the brightness and contrast of a selected image.
     * Shown when `showBrightnessContrastDialogState` is true and an image URI is available
     * in `imageToEditForAdjustment`.
     * Passes the image URI, loading state, and error state to the dialog.
     * Calls ViewModel functions to apply changes or dismiss the dialog.
     */

                Button(
                    onClick = {
                        Log.d("GalleryClick", "Select from Gallery clicked.")
                        when {
                            storagePermissionsState.allPermissionsGranted -> {
                                Log.d("GalleryClick", "Permissions GRANTED. Calling viewModel.onSelectFromGalleryClicked(true)")
                                viewModel.onSelectFromGalleryClicked(true)
                            }
                            storagePermissionsState.permissions.any { it.status.shouldShowRationale } -> {
                                Log.d("GalleryClick", "Permissions RATIONALE. Showing dialog.")
                                showStorageRationaleDialog = true
                            }
                            else -> {
                                Log.d("GalleryClick", "Permissions NOT GRANTED. Launching request.")
                                storagePermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    enabled = !isCreatingPdf
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = stringResource(R.string.select_from_gallery))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.select_from_gallery))
                }
            }

            // Add Google Drive Button for Premium Users
            if (isPremiumUser) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Navigate to Google Drive Screen
                        navController.navigate(com.insnaejack.pdfgenerator.ui.navigation.AppDestinations.GOOGLE_DRIVE_ROUTE)
                    },
                    modifier = Modifier.fillMaxWidth() // Or adjust width as needed
                ) {
                    // Consider adding a Google Drive icon
                    Icon(Icons.Filled.CloudDownload, contentDescription = stringResource(R.string.import_from_google_drive)) // Placeholder icon
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.import_from_google_drive))
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            if (isCreatingPdf) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 20.dp))
                Text(stringResource(R.string.creating_pdf))
            } else if (selectedImageUris.isNotEmpty()) {
                Text(
                    stringResource(R.string.selected_images_count, selectedImageUris.size),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    modifier = Modifier.height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                   items(selectedImageUris, key = { it.toString() }) { uri -> // Add key for stability
                       SelectedImageItem(
                           uri = uri,
                           onRemoveClick = { viewModel.removeImageUri(uri) },
                           onEditClick = { viewModel.onEditImageClicked(uri) }, // Trigger uCrop
                           onAdjustClick = { viewModel.onAdjustImageClicked(uri) } // Trigger Adjust Dialog
                      )
                 }
             }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.createPdfFromSelectedImages() },
                    enabled = selectedImageUris.isNotEmpty() && !isCreatingPdf
                ) {
                    Text(stringResource(R.string.create_pdf))
                }
            } else {
                Column(
                    modifier = Modifier.weight(1f), // Use weight to push to center if no images
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "No images selected", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.no_images_selected), style = MaterialTheme.typography.bodyLarge)
                }
            }
            // Spacer to push content up if ad is present or not
            if (isPremiumUser) Spacer(modifier = Modifier.height(50.dp)) // Approx height of ad
        }
    }

    if (showCameraRationaleDialog) {
        PermissionRationaleDialog(
            title = "Camera Permission",
            text = stringResource(R.string.permission_camera_rationale),
            onConfirm = {
                showCameraRationaleDialog = false
                cameraPermissionState.launchPermissionRequest()
            },
            onDismiss = { showCameraRationaleDialog = false }
        )
    }

    if (showStorageRationaleDialog) {
        PermissionRationaleDialog(
            title = "Storage Permission",
            text = stringResource(R.string.permission_storage_rationale),
            onConfirm = {
                showStorageRationaleDialog = false
                storagePermissionsState.launchMultiplePermissionRequest()
            },
            onDismiss = { showStorageRationaleDialog = false }
        )
    }

    // Ensure only one block for showing PdfSettingsDialog
    if (showPdfSettingsDialog) {
        PdfSettingsDialog(
            currentSettings = currentPdfSettings,
            isPremiumUser = isPremiumUser,
            onDismissRequest = { showPdfSettingsDialog = false },
            onSettingsChanged = { newSettings ->
                viewModel.updatePdfSettings(newSettings)
                if(isPremiumUser) {
                    Toast.makeText(context, "PDF settings updated.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upgrade to premium to apply custom PDF settings.", Toast.LENGTH_LONG).show()
                }
            }
       )
   }

   // Observe ViewModel state for showing brightness/contrast dialog
   val imageToEditForAdjustment by viewModel.imageToEditUri.collectAsState()
   val isApplyingEdit by viewModel.isApplyingEdit.collectAsState()
   val editError by viewModel.editError.collectAsState()

   if (showBrightnessContrastDialogState && imageToEditForAdjustment != null) {
       BrightnessContrastDialog(
           imageUri = imageToEditForAdjustment!!, // Pass the URI to the dialog
           isApplying = isApplyingEdit,
           error = editError,
           onDismiss = { viewModel.onBrightnessContrastDialogDismissed() },
           onApply = { uri, brightness, contrast -> // Receive URI back from dialog
               viewModel.applyBrightnessContrast(uri, brightness, contrast)
           },
           onConsumeError = { viewModel.consumeEditError() }
       )
   }
}

@Composable
/**
 * Composable function to display a single selected image thumbnail.
 * Shows the image, a remove button (top-right), and a row of edit buttons (bottom-center)
 * for crop/rotate and brightness/contrast adjustments.
 *
 * @param uri The URI of the image to display.
 * @param onRemoveClick Lambda to be invoked when the remove button is clicked.
 * @param onEditClick Lambda to be invoked when the crop/rotate edit button is clicked.
 * @param onAdjustClick Lambda to be invoked when the brightness/contrast adjust button is clicked.
 */
fun SelectedImageItem(
   uri: Uri,
   onRemoveClick: () -> Unit,
   onEditClick: () -> Unit,
   onAdjustClick: () -> Unit
) {
   Box(
       modifier = Modifier
           .size(120.dp) // Slightly larger to accommodate buttons
           .padding(4.dp)
   ) {
        Image(
            painter = rememberAsyncImagePainter(model = uri),
            contentDescription = "Selected Image",
            modifier = Modifier.fillMaxSize(),
           contentScale = ContentScale.Crop
       )
       // Remove Button (Top Right)
       IconButton(
           onClick = onRemoveClick,
           modifier = Modifier
               .align(Alignment.TopEnd)
               .padding(2.dp)
               .size(24.dp)
               .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = CircleShape)
       ) {
           Icon(
               Icons.Filled.Close,
               contentDescription = stringResource(R.string.remove_image),
               tint = MaterialTheme.colorScheme.onSurface,
               modifier = Modifier.size(16.dp) // Smaller icon
           )
       }

       // Edit Buttons Row (Bottom Center)
       Row(
           modifier = Modifier
               .align(Alignment.BottomCenter)
               .fillMaxWidth()
               .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
               .padding(vertical = 2.dp),
           horizontalArrangement = Arrangement.SpaceEvenly
       ) {
           // Edit (Crop/Rotate) Button
           IconButton(
               onClick = onEditClick,
               modifier = Modifier.size(32.dp)
           ) {
               Icon(
                   Icons.Outlined.CropRotate,
                   contentDescription = "Crop/Rotate",
                   tint = MaterialTheme.colorScheme.onSurface,
                   modifier = Modifier.size(20.dp)
               )
           }
           // Adjust (Brightness/Contrast) Button
           IconButton(
               onClick = onAdjustClick,
               modifier = Modifier.size(32.dp)
           ) {
               Icon(
                   Icons.Outlined.ColorLens, // Or Tune icon
/**
 * A generic AlertDialog composable for displaying permission rationales.
 *
 * @param title The title of the dialog.
 * @param text The rationale message explaining why the permission is needed.
 * @param onConfirm Lambda to be invoked when the user confirms (e.g., to proceed with permission request).
 * @param onDismiss Lambda to be invoked when the user dismisses the dialog.
 */
                   contentDescription = "Adjust",
                   tint = MaterialTheme.colorScheme.onSurface,
                   modifier = Modifier.size(20.dp)
               )
           }
       }
   }
}

@Composable
fun PermissionRationaleDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
/**
 * Composable function to display a banner ad using AdMob.
 * Uses AndroidView to embed the AdView. Configures the ad unit ID, ad size,
 * and an AdListener to log ad events (load success/failure, open, click, close).
 * Loads an ad request when the view is created.
 *
 * @param modifier Modifier for layout customization.
 */
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun AdvertView(modifier: Modifier = Modifier) {
    // Use a test ad unit ID for development. Replace with your actual ad unit ID for production.
    // Test Ad Unit ID for Banner: ca-app-pub-3940256099942544/6300978111
    val adUnitId = "ca-app-pub-6349011183583557/3269618989" // Your Ad Unit ID
    AndroidView(
        modifier = modifier.height(50.dp), // Standard banner height
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("AdvertView", "Ad loaded successfully.")
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("AdvertView", "Ad failed to load: ${adError.message} (Code: ${adError.code})")
                        Log.e("AdvertView", "Domain: ${adError.domain}, Cause: ${adError.cause}")
                        adError.responseInfo?.let {
                            Log.e("AdvertView", "Response Info: ${it.mediationAdapterClassName} - ${it.responseId}")
                        }
                    }

                    override fun onAdOpened() {
                        Log.d("AdvertView", "Ad opened.")
                    }

                    override fun onAdClicked() {
                        Log.d("AdvertView", "Ad clicked.")
                    }

                    override fun onAdClosed() {
                        Log.d("AdvertView", "Ad closed.")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            Log.d("AdvertView", "AdView updated or recomposed. Current adUnitId: ${adView.adUnitId}")
            // Consider re-calling loadAd if essential properties change, though typically not needed for banners.
        }
    )
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    PdfGeneratorTheme {
        MainScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun SelectedImageItemPreview() {
  PdfGeneratorTheme {
      SelectedImageItem(uri = Uri.EMPTY, onRemoveClick = {}, onEditClick = {}, onAdjustClick = {})
  }
}

// Need to add imports for ContextCompat, Color, etc. if not already present
// import androidx.core.content.ContextCompat
// import androidx.compose.ui.graphics.Color
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.material.icons.outlined.ColorLens
// import androidx.compose.material.icons.outlined.CropRotate
// import androidx.compose.ui.unit.sp

@Preview(showBackground = true)
@Composable
fun AdvertViewPreview(){
   PdfGeneratorTheme {
       AdvertView(modifier = Modifier.fillMaxWidth())
   }
}

@Composable
fun BrightnessContrastDialog(
   imageUri: Uri,
   isApplying: Boolean,
   error: String?,
   onDismiss: () -> Unit,
   onApply: (uri: Uri, brightness: Float, contrast: Float) -> Unit,
   onConsumeError: () -> Unit
) {
   var brightness by remember { mutableStateOf(0f) } // Range e.g., -0.5f to 0.5f or as needed
   var contrast by remember { mutableStateOf(1f) }   // Range e.g., 0.5f to 1.5f (1f is no change)

   AlertDialog(
       onDismissRequest = { if (!isApplying) onDismiss() },
       title = { Text("Adjust Brightness & Contrast") },
       text = {
           Column {
               if (isApplying) {
                   Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                       CircularProgressIndicator()
                   }
               } else {
                   // Brightness Slider
                   Text("Brightness: ${String.format("%.2f", brightness * 100)}%") // Show as percentage
                   Slider(
                       value = brightness,
                       onValueChange = { brightness = it },
                       valueRange = -0.5f..0.5f, // Adjusted range for finer control around 0
                       steps = 19 // (0.5 - (-0.5)) / 0.05 - 1 = 1/0.05 -1 = 20-1 = 19 steps for 0.05 increments
                   )
                   Spacer(modifier = Modifier.height(16.dp))

                   // Contrast Slider
                   Text("Contrast: ${String.format("%.2f", contrast)}")
                   Slider(
                       value = contrast,
                       onValueChange = { contrast = it },
                       valueRange = 0.5f..1.5f, // Adjusted range for finer control around 1
                       steps = 19 // (1.5 - 0.5) / 0.05 - 1 = 1/0.05 -1 = 19 steps for 0.05 increments
                   )
               }
               error?.let {
                   Spacer(modifier = Modifier.height(8.dp))
                   Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                   // Consider a button to dismiss the error explicitly if auto-consumption is not desired
                   TextButton(onClick = onConsumeError, modifier = Modifier.align(Alignment.End)) {
                       Text("Dismiss Error")
                   }
               }
           }
       },
       confirmButton = {
           TextButton(
               onClick = { onApply(imageUri, brightness, contrast) }, // Pass imageUri back
               enabled = !isApplying
           ) {
               Text("Apply")
           }
       },
       dismissButton = {
           TextButton(
               onClick = { onDismiss() },
               enabled = !isApplying
           ) {
               Text("Cancel")
           }
       }
   )
}