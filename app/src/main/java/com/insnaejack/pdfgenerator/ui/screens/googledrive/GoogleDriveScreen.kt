package com.insnaejack.pdfgenerator.ui.screens.googledrive

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background // Specific import for background modifier
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Changed import
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.insnaejack.pdfgenerator.R
import com.google.api.services.drive.model.File as DriveFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveScreen(
    navController: NavController,
    viewModel: GoogleDriveViewModel = hiltViewModel(),
    onFilesSelected: (List<Uri>) -> Unit, // Changed callback to return List<Uri>
) {
    val context = LocalContext.current
    val account by viewModel.googleAccount.collectAsState()
    val driveFiles by viewModel.driveFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState() // Add downloading state
    val error by viewModel.error.collectAsState()
    val downloadedUris by viewModel.downloadedUris.collectAsState() // Observe downloaded URIs
    var selectedFileIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val signedInAccount = task.getResult(ApiException::class.java)
                viewModel.setGoogleAccount(signedInAccount)
                Log.i("GoogleDriveScreen", "Sign-in successful for ${signedInAccount?.email}")
            } catch (e: ApiException) {
                Log.e("GoogleDriveScreen", "Google sign in failed", e)
                viewModel.setError("Google Sign-In failed: ${e.statusCode}")
            }
        } else {
            Log.w("GoogleDriveScreen", "Sign-in flow cancelled or failed. Result code: ${result.resultCode}")
            viewModel.setError("Sign-In cancelled or failed.")
        }
    }

    // Attempt silent sign-in or check existing permissions on launch
    LaunchedEffect(Unit) {
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastSignedInAccount != null && GoogleSignIn.hasPermissions(
                lastSignedInAccount,
                *viewModel.driveScope.toTypedArray(),
            )
        ) {
            Log.i(
                "GoogleDriveScreen",
                "Found previously signed in account with permissions: ${lastSignedInAccount.email}",
            )
            viewModel.setGoogleAccount(lastSignedInAccount)
        } else {
            Log.i(
                "GoogleDriveScreen",
                "No valid signed-in account found or permissions missing. Need explicit sign-in.",
            )
            // Optionally trigger sign-in automatically, or wait for user action
            // signInLauncher.launch(viewModel.getSignInIntent())
        }
    }

    LaunchedEffect(account) {
        if (account != null) {
            viewModel.loadDriveFiles()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select from Google Drive") }, // TODO: Add to strings.xml
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: Add to strings.xml
                    }
                },
                actions = {
                    if (account != null) {
                        TextButton(onClick = { viewModel.signOut() }) {
                            Text("Sign Out") // TODO: Add to strings.xml
                        }
                    }
                    if (selectedFileIds.isNotEmpty()) {
                        Button(
                            onClick = {
                                Log.d("GoogleDriveScreen", "Add button clicked. Processing files: $selectedFileIds")
                                viewModel.processSelectedFiles(selectedFileIds)
                            },
                            enabled = !isDownloading, // Disable button while downloading
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Downloading...") // TODO: Add to strings.xml
                            } else {
                                Text("Add (${selectedFileIds.size})") // TODO: Add to strings.xml
                            }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (account == null) {
                Text("Please sign in to access Google Drive.", textAlign = TextAlign.Center) // TODO: Add to strings.xml
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { signInLauncher.launch(viewModel.getSignInIntent()) }) {
                    Text("Sign in with Google") // TODO: Add to strings.xml
                }
            } else if (isLoading) {
                CircularProgressIndicator()
                Text("Loading files...") // TODO: Add to strings.xml
            } else if (error != null) {
                Icon(
                    Icons.Filled.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                ) // TODO: Add to strings.xml
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.loadDriveFiles() }) {
                    Text("Retry") // TODO: Add to strings.xml
                }
            } else if (driveFiles.isEmpty()) {
                Icon(
                    Icons.Filled.ImageSearch,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "No image files found in your Google Drive.",
                    textAlign = TextAlign.Center,
                ) // TODO: Add to strings.xml
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(driveFiles, key = { it.id }) { file ->
                        DriveFileItem(
                            file = file,
                            isSelected = selectedFileIds.contains(file.id),
                            onItemClick = { fileId ->
                                selectedFileIds = if (selectedFileIds.contains(fileId)) {
                                    selectedFileIds - fileId
                                } else {
                                    selectedFileIds + fileId
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    // Handle downloaded URIs
    LaunchedEffect(downloadedUris) {
        downloadedUris?.let { uris ->
            if (uris.isNotEmpty()) {
                Log.i("GoogleDriveScreen", "Passing back downloaded URIs: $uris")
                onFilesSelected(uris) // Pass the downloaded URIs back
                navController.popBackStack() // Navigate back after processing
            }
            // Consume the state even if empty/failed to prevent re-triggering on recomposition
            viewModel.consumeDownloadedUris()
        }
    }
}

@Composable
fun DriveFileItem(
    file: DriveFile,
    isSelected: Boolean,
    onItemClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // Make items square
            .clickable { onItemClick(file.id) }
            .then(
                if (isSelected) {
                    Modifier.border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.small,
                    )
                } else {
                    Modifier
                },
            )
            .padding(2.dp), // Padding inside the border
    ) {
        Image(
            // Use thumbnailLink if available, otherwise a placeholder
            painter = rememberAsyncImagePainter(
                model = file.thumbnailLink
                    ?: R.drawable.ic_launcher_foreground, // Replace with a proper placeholder drawable
                error = painterResource(id = R.drawable.ic_launcher_foreground), // Placeholder on error
            ),
            contentDescription = file.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Selected", // TODO: Add to strings.xml
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.small,
                    ),
            )
        }
    }
}
