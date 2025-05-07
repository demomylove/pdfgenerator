package com.insnaejack.pdfgenerator.ui.screens.googledrive

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insnaejack.pdfgenerator.google.GoogleDriveService
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import javax.inject.Inject
import com.google.api.services.drive.model.File as DriveFile

@HiltViewModel
class GoogleDriveViewModel @Inject constructor(
    private val driveService: GoogleDriveService
) : ViewModel() {

    private val _googleAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val googleAccount: StateFlow<GoogleSignInAccount?> = _googleAccount.asStateFlow()

    private val _driveFiles = MutableStateFlow<List<DriveFile>>(emptyList())
    val driveFiles: StateFlow<List<DriveFile>> = _driveFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDownloading = MutableStateFlow(false) // State for download progress
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State to hold downloaded URIs ready to be passed back
    private val _downloadedUris = MutableStateFlow<List<Uri>?>(null)
    val downloadedUris: StateFlow<List<Uri>?> = _downloadedUris.asStateFlow()


    val driveScope = listOf(Scope(DriveScopes.DRIVE_READONLY)) // Match scope used in service

    fun getSignInIntent(): Intent {
        return driveService.googleSignInClient.signInIntent
    }

    fun setGoogleAccount(account: GoogleSignInAccount?) {
        _googleAccount.value = account
        if (account == null) {
            // Clear files if user signs out
            _driveFiles.value = emptyList()
            _error.value = null
        }
    }

    fun loadDriveFiles() {
        val account = _googleAccount.value
        if (account == null) {
            _error.value = "Not signed in." // TODO: Add to strings.xml
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _driveFiles.value = emptyList() // Clear previous results

            val drive = driveService.getDriveService(account)
            if (drive == null) {
                _error.value = "Failed to initialize Google Drive service." // TODO: Add to strings.xml
                _isLoading.value = false
                return@launch
            }

            try {
                val files = driveService.listImageFiles(drive)
                _driveFiles.value = files
                if (files.isEmpty()) {
                    Log.i("GoogleDriveVM", "No image files found.")
                    // Optionally set a specific state/message for no files found
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveVM", "Error loading drive files", e)
                _error.value = "Error loading files: ${e.localizedMessage}" // TODO: Add to strings.xml
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            driveService.signOut()
            setGoogleAccount(null) // Clear account state in ViewModel
        }
    }

    fun setError(message: String?) {
        _error.value = message
    }

    fun processSelectedFiles(fileIds: Set<String>) {
        val account = _googleAccount.value ?: return // Need account
        val filesToDownload = _driveFiles.value.filter { fileIds.contains(it.id) }
        if (filesToDownload.isEmpty()) return

        viewModelScope.launch {
            _isDownloading.value = true
            _error.value = null
            val drive = driveService.getDriveService(account)
            if (drive == null) {
                _error.value = "Failed to initialize Google Drive service."
                _isDownloading.value = false
                return@launch
            }

            val downloadJobs = mutableListOf<Deferred<Uri?>>()
            filesToDownload.forEach { file ->
                downloadJobs.add(async { driveService.downloadFile(drive, file) })
            }

            try {
                val results = downloadJobs.awaitAll()
                val successfulUris = results.filterNotNull()
                if (successfulUris.isNotEmpty()) {
                    _downloadedUris.value = successfulUris
                } else {
                    _error.value = "Failed to download selected files." // Or more specific error
                }
                Log.i("GoogleDriveVM", "Downloaded URIs: $successfulUris")
            } catch (e: Exception) {
                 Log.e("GoogleDriveVM", "Error during file downloads", e)
                _error.value = "Error downloading files: ${e.localizedMessage}"
            } finally {
                _isDownloading.value = false
            }
        }
    }

     fun consumeDownloadedUris() {
        _downloadedUris.value = null
    }
}