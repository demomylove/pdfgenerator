package com.insnaejack.pdfgenerator.google

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "GoogleDriveService"

        // Request read-only access to files created or opened by the app.
        // Use DriveScopes.DRIVE_FILE for broader access if needed, but request minimal scope first.
        private val DRIVE_SCOPES = listOf(DriveScopes.DRIVE_READONLY)
    }

    // Configure Google Sign In
    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            // Request ID token if you need to authenticate with a backend server
            // .requestIdToken(context.getString(R.string.server_client_id))
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY)) // Request Drive scope
            // Add other scopes if needed, e.g., DriveScopes.DRIVE_FILE
            .build()
    }

    val googleSignInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun getDriveService(account: GoogleSignInAccount): Drive? {
        return withContext(Dispatchers.IO) {
            try {
                val credential = GoogleAccountCredential.usingOAuth2(context, DRIVE_SCOPES)
                account.email?.let { credential.setSelectedAccountName(it) } ?: account.account?.name?.let { credential.setSelectedAccountName(it) }
                Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential,
                )
                    .setApplicationName("PDF Generator") // Replace with your app name
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get Drive service", e)
                null
            }
        }
    }

    // Example function to list image files (adjust query as needed)
    suspend fun listImageFiles(driveService: Drive): List<com.google.api.services.drive.model.File> {
        return withContext(Dispatchers.IO) {
            try {
                // Query for image files visible to the app
                val query = "mimeType contains 'image/' and trashed = false"
                val result = driveService.files().list()
                    .setSpaces("drive") // Search only within the user's Drive space
                    .setFields("nextPageToken, files(id, name, mimeType, thumbnailLink, webContentLink)") // Specify fields needed
                    .setQ(query)
                    .setPageSize(20) // Adjust page size as needed
                    .execute()
                result.files ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to list files from Drive", e)
                emptyList()
            }
        }
    }

    suspend fun downloadFile(driveService: Drive, file: com.google.api.services.drive.model.File): Uri? {
        return withContext(Dispatchers.IO) {
            val fileId = file.id
            if (fileId == null) {
                Log.e(TAG, "File ID is null, cannot download.")
                return@withContext null
            }
            try {
                // Create a temporary file in the cache directory
                val extension = when (file.mimeType) {
                    "image/jpeg" -> ".jpg"
                    "image/png" -> ".png"
                    "image/gif" -> ".gif"
                    "image/webp" -> ".webp"
                    else -> ".img" // Default extension
                }
                val tempFile = File.createTempFile(
                    "drive_${fileId}_", // Prefix
                    extension, // Suffix
                    context.cacheDir, // Directory
                )

                // Get file content input stream
                val inputStream: InputStream = driveService.files().get(fileId).executeMediaAsInputStream()
                // Write stream to temporary file
                val outputStream: OutputStream = FileOutputStream(tempFile)

                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                Log.i(TAG, "File downloaded successfully to: ${tempFile.absolutePath}")
                tempFile.toUri() // Return the Uri of the downloaded temporary file
            } catch (e: IOException) {
                Log.e(TAG, "Failed to download file ID: $fileId", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "An unexpected error occurred during download for file ID: $fileId", e)
                null
            }
        }
    }

    // TODO: Implement logic to clear cached downloaded files periodically if needed
    // TODO: Implement file download logic if needed
    // suspend fun downloadFile(driveService: Drive, fileId: String): InputStream? { ... }

    suspend fun signOut() {
        withContext(Dispatchers.Main) { // Sign out needs to be on Main thread sometimes
            try {
                googleSignInClient.signOut().addOnCompleteListener {
                    Log.i(TAG, "Google Sign-Out successful.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during Google Sign-Out", e)
            }
        }
    }
}
