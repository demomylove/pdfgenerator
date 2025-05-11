package com.insnaejack.pdfgenerator.ui.screens.mainscreen

import android.Manifest
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
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val premiumProduct = premiumProductDetailsMap[ProductIds.PREMIUM_UPGRADE_ID]
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
                        Toast.makeText(context, "Purchase cancelled", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Purchase failed: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                viewModel.billingClientWrapper.consumePurchaseUpdateEvent()
            }
        }
    }

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
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addSelectedImageUris(uris)
            }
        }
    )
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri>? ->
            uris?.let { viewModel.addSelectedImageUris(it) }
        }
    )

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
        if (triggerGalleryLaunch) {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                pickMultipleMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                getContentLauncher.launch("image/*")
            }
            viewModel.onGalleryLaunchTriggered()
        }
    }

    LaunchedEffect(pdfCreationStatus) {
        pdfCreationStatus?.let { result ->
            val message = if (result.success) {
                context.getString(R.string.pdf_created_successfully, result.filePath ?: "N/A")
            } else {
                context.getString(R.string.error_creating_pdf, result.errorMessage ?: "Unknown error")
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            if (result.success) {
                viewModel.clearSelectedImages()
            }
            viewModel.consumePdfCreationStatus()
        }
    }

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
                    if (!isPremiumUser && premiumProduct != null) {
                        IconButton(onClick = {
                            activity?.let { act ->
                                viewModel.billingClientWrapper.launchPurchaseFlow(act, premiumProduct.productDetails)
                            }
                        }) {
                            Icon(Icons.Outlined.WorkspacePremium, contentDescription = stringResource(R.string.upgrade_to_premium))
                        }
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
        }
    ) { paddingValues ->
        Column(
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
                }) {
                    Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("${stringResource(R.string.upgrade_to_premium)} - ${premiumProduct.formattedPrice}")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
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

                Button(
                    onClick = {
                        when {
                            storagePermissionsState.allPermissionsGranted -> viewModel.onSelectFromGalleryClicked(true)
                            storagePermissionsState.permissions.any { it.status.shouldShowRationale } -> showStorageRationaleDialog = true
                            else -> storagePermissionsState.launchMultiplePermissionRequest()
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
                    items(selectedImageUris) { uri ->
                        SelectedImageItem(uri = uri, onRemoveClick = { viewModel.removeImageUri(uri) })
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
}

@Composable
fun SelectedImageItem(uri: Uri, onRemoveClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .padding(4.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = uri),
            contentDescription = "Selected Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = MaterialTheme.shapes.small)
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_image), tint = MaterialTheme.colorScheme.onSurface)
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
        SelectedImageItem(uri = Uri.EMPTY, onRemoveClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun AdvertViewPreview(){
    PdfGeneratorTheme {
        AdvertView(modifier = Modifier.fillMaxWidth())
    }
}