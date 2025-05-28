package com.insnaejack.pdfgenerator.ui.screens.pdflist

import android.app.Application
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.insnaejack.pdfgenerator.model.ManagedPdfFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine // Added import
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// Define a constant for the directory where PDFs are stored.
// This should ideally be a more robust solution, perhaps configured elsewhere
// or using app-specific directories.
const val PDF_DIRECTORY_NAME = "GeneratedPDFs"

// Define items for the UI list
sealed class DisplayItem {
    data class FolderItem(val name: String, val path: String) : DisplayItem()
    data class FileItem(val file: ManagedPdfFile) : DisplayItem()
}


@HiltViewModel
class PdfListViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {

    // State for the current logical path
    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // Holds ALL managed files, regardless of folder
    private val _allPdfFiles = MutableStateFlow<List<ManagedPdfFile>>(emptyList())

    // Holds items (folders and files) to be displayed for the current path
    private val _displayedItems = MutableStateFlow<List<DisplayItem>>(emptyList())
    val displayedItems: StateFlow<List<DisplayItem>> = _displayedItems.asStateFlow()

    // Commenting out old state, replaced by displayedItems
    // private val _pdfFiles = MutableStateFlow<List<ManagedPdfFile>>(emptyList())
    // val pdfFiles: StateFlow<List<ManagedPdfFile>> = _pdfFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Combine flows to update displayedItems whenever allFiles or currentPath changes
        viewModelScope.launch {
            _allPdfFiles.combine(_currentPath) { allFiles, path ->
                calculateDisplayedItems(allFiles, path)
            }.collect { items ->
                _displayedItems.value = items
            }
        }
        loadPdfFiles() // Initial load
    }

    // New function to calculate items for the current path
    private fun calculateDisplayedItems(allFiles: List<ManagedPdfFile>, currentPath: String): List<DisplayItem> {
        val items = mutableListOf<DisplayItem>()

        // Find subfolders directly under the current path
        val subFolders = allFiles
            .filter { it.folderPath.startsWith(currentPath) && it.folderPath != currentPath } // Files in sub-paths
            .mapNotNull {
                // Extract the part of the path immediately after currentPath
                val remainingPath = it.folderPath.removePrefix(currentPath)
                remainingPath.substringBefore('/').ifBlank { null } // Get the first segment
            }
            .distinct() // Unique folder names
            .sorted()
            .map { folderName ->
                DisplayItem.FolderItem(name = folderName, path = currentPath + folderName + "/")
            }

        // Find files directly within the current path
        val filesInCurrentPath = allFiles
            .filter { it.folderPath == currentPath }
            .sortedByDescending { it.lastModified } // Keep original sorting for files
            .map { DisplayItem.FileItem(it) }

        items.addAll(subFolders)
        items.addAll(filesInCurrentPath)
        return items
    }


    // Modify loadPdfFiles to populate _allPdfFiles
    fun loadPdfFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val pdfDir = File(application.getExternalFilesDir(null), PDF_DIRECTORY_NAME)
                if (!pdfDir.exists()) {
                    pdfDir.mkdirs()
                }
                val files = pdfDir.listFiles { _, name -> name.endsWith(".pdf", ignoreCase = true) }
                // TODO: Load folderPath information - for now, assume all loaded files are in root "/"
                // This needs integration with data persistence (DataStore/Room) later
                _allPdfFiles.value = files?.mapNotNull { file ->
                    val authority = "${application.packageName}.provider"
                    val uri = FileProvider.getUriForFile(application, authority, file)
                    // Assuming files loaded directly are in root until persistence is added
                    ManagedPdfFile(
                        name = file.name,
                        uri = uri,
                        filePath = file.absolutePath,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        folderPath = "/" // Assign default path for now
                    )
                }?.toList() ?: emptyList() // Removed sorting here, will be handled in calculateDisplayedItems if needed

            } catch (e: Exception) {
                _error.value = "Failed to load PDF files: ${e.message}"
                _allPdfFiles.value = emptyList() // Clear list on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Navigation functions
    fun navigateToFolder(folderPath: String) {
        // Basic validation: ensure it's a valid folder path format
        if (folderPath.endsWith("/") && folderPath.startsWith("/")) {
             _currentPath.value = folderPath
        } else {
            // Handle error or log warning - invalid path format
            _error.value = "Invalid folder path format: $folderPath"
        }
    }

    fun navigateUp() {
        val current = _currentPath.value
        if (current != "/") {
            // Find the parent path by removing the last segment
            val parentPath = current.trimEnd('/').substringBeforeLast('/', "/") + "/"
             _currentPath.value = parentPath
        }
        // Cannot go up from root "/"
    }


    // Update deletePdfFile to modify _allPdfFiles
    fun deletePdfFile(fileToDelete: ManagedPdfFile) {
        viewModelScope.launch {
            try {
                val file = File(fileToDelete.filePath)
                if (file.exists()) {
                    if (file.delete()) {
                        // Refresh the list by removing from _allPdfFiles
                        _allPdfFiles.value = _allPdfFiles.value.filterNot { it.filePath == fileToDelete.filePath }
                        // displayedItems will update automatically via the combine flow
                    } else {
                        _error.value = "Failed to delete file: ${fileToDelete.name}"
                    }
                } else {
                    _error.value = "File not found: ${fileToDelete.name}"
                    // Also remove from list if it's somehow there but doesn't exist
                     _allPdfFiles.value = _allPdfFiles.value.filterNot { it.filePath == fileToDelete.filePath }
                }
            } catch (e: Exception) {
                _error.value = "Error deleting file ${fileToDelete.name}: ${e.message}"
            }
        }
    }

    // Update renamePdfFile to modify _allPdfFiles
    fun renamePdfFile(fileToRename: ManagedPdfFile, newName: String) {
        viewModelScope.launch {
            _error.value = null // Clear previous errors
            if (!newName.endsWith(".pdf", ignoreCase = true)) {
                _error.value = "New name must end with .pdf"
                return@launch
            }
            // Ensure newName doesn't contain path separators for logical paths
            if (newName.isBlank() || newName.contains('/')) {
                _error.value = "Invalid file name for logical path."
                return@launch
            }

            val oldFile = File(fileToRename.filePath)
            val parentDir = oldFile.parentFile
            if (parentDir == null) {
                _error.value = "Cannot determine parent directory for physical file."
                return@launch
            }
            val newPhysicalFile = File(parentDir, newName) // Physical file rename

            // Check for physical file collision
            if (newPhysicalFile.exists()) {
                _error.value = "A physical file with the name '$newName' already exists."
                return@launch
            }

            // Check for logical file collision within the same folderPath
            val collisionExists = _allPdfFiles.value.any {
                it.folderPath == fileToRename.folderPath && it.name == newName && it.filePath != fileToRename.filePath
            }
            if (collisionExists) {
                 _error.value = "A file with the name '$newName' already exists in this folder."
                 return@launch
            }


            try {
                if (oldFile.renameTo(newPhysicalFile)) {
                    // Update the specific item in _allPdfFiles
                    _allPdfFiles.value = _allPdfFiles.value.map {
                        if (it.filePath == fileToRename.filePath) {
                            it.copy(name = newName, filePath = newPhysicalFile.absolutePath) // Update name and physical path
                        } else {
                            it
                        }
                    }
                    // displayedItems will update automatically
                } else {
                    _error.value = "Failed to rename physical file: ${fileToRename.name}"
                }
            } catch (e: SecurityException) {
                _error.value = "Permission denied while renaming file: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Error renaming file ${fileToRename.name}: ${e.message}"
            }
        }
    }

    // Temporary function for creating a folder (validation only, no persistence)
    fun createFolder(parentPath: String, newFolderName: String) {
        _error.value = null // Clear previous errors

        // Validate name
        if (newFolderName.isBlank() || newFolderName.contains('/')) {
            _error.value = "Invalid folder name."
            return
        }

        // Check for collision in the current displayed items for the parent path
        // Note: This assumes calculateDisplayedItems is up-to-date for the parentPath
        // A more robust check might involve directly querying _allPdfFiles based on parentPath
        val collisionExists = _displayedItems.value.any { item ->
            when(item) {
                is DisplayItem.FolderItem -> item.name == newFolderName
                is DisplayItem.FileItem -> item.file.name == newFolderName && item.file.folderPath == parentPath // Check files only in the target parent path
            }
        }

        if (collisionExists) {
            _error.value = "A folder or file with the name '$newFolderName' already exists here."
            return
        }

        // --- Temporary Action ---
        // Since folders are derived from file paths, we don't add anything to _allPdfFiles yet.
        // The folder will appear once a file is moved into its path.
        // For now, just log success or potentially update a transient state if needed for UI feedback.
        println("Temporary createFolder validation successful for: $parentPath$newFolderName/")
        // You could set a temporary success message via another StateFlow if needed.
        // --- End Temporary Action ---

        // TODO: Replace temporary action with actual persistence logic (e.g., DataStore update or placeholder creation)
        // TODO: Implement moveFile function to actually put files into the new folder path.
    }

    // TODO: Implement moveFile which will require data persistence logic
}
