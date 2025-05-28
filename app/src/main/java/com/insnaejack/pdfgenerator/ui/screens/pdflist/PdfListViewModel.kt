package com.insnaejack.pdfgenerator.ui.screens.pdflist

import android.app.Application
import android.util.Log // Add Log import
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

// Define sorting criteria and order
enum class SortCriteria {
    ByName, ByDate, BySize // Add BySize
}

enum class SortOrder {
    Ascending, Descending
}

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

    private val TAG = "PdfListViewModel" // Tag for logging

    // State for the current logical path
    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    // State for sorting
    private val _sortCriteria = MutableStateFlow(SortCriteria.ByDate) // Default sort by date
    val sortCriteria: StateFlow<SortCriteria> = _sortCriteria.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.Descending) // Default sort descending
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    // Holds ALL managed files, regardless of folder
    private val _allPdfFiles = MutableStateFlow<List<ManagedPdfFile>>(emptyList())

    // Holds items (folders and files) to be displayed for the current path
    private val _displayedItems = MutableStateFlow<List<DisplayItem>>(emptyList())
    val displayedItems: StateFlow<List<DisplayItem>> = _displayedItems.asStateFlow()

    // State for search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Commenting out old state, replaced by displayedItems
    // private val _pdfFiles = MutableStateFlow<List<ManagedPdfFile>>(emptyList())
    // val pdfFiles: StateFlow<List<ManagedPdfFile>> = _pdfFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine all relevant flows to trigger recalculation of displayed items
            combine(
                _allPdfFiles,
                _currentPath,
                _sortCriteria,
                _sortOrder,
                _searchQuery, // Include search query
            ) { allFiles, path, criteria, order, query ->
                calculateDisplayedItems(allFiles, path, criteria, order, query) // Pass query
            }.collect { items ->
                _displayedItems.value = items
            }
        }
        loadPdfFiles() // Initial load
    }

    // New function to calculate items for the current path, applying sorting and filtering
    private fun calculateDisplayedItems(
        allFiles: List<ManagedPdfFile>, // Receive the full list from the combine trigger
        currentPath: String,
        sortCriteria: SortCriteria,
        sortOrder: SortOrder,
        searchQuery: String, // Add search query parameter
    ): List<DisplayItem> {
        val items = mutableListOf<DisplayItem>()
        val normalizedQuery = searchQuery.trim().lowercase() // Normalize query for case-insensitive search

        // --- Filtering based on search query ---
        // Filter the input allFiles list based on the query
        val filteredFiles = if (normalizedQuery.isBlank()) {
            allFiles // No query, use the input list
        } else {
            allFiles.filter {
                it.name.lowercase().contains(normalizedQuery) // Simple name contains check
            }
        }
        // --- End Filtering ---

        // Find subfolders directly under the current path (using filtered files to determine relevant folders)
        // Only show folders if search query is blank OR if a file matching the query exists within that folder structure
        val subFolders = allFiles // Use original allFiles to discover all potential subfolders
            .filter { it.folderPath.startsWith(currentPath) && it.folderPath != currentPath } // Files in sub-paths
            .mapNotNull {
                // Extract the part of the path immediately after currentPath
                val remainingPath = it.folderPath.removePrefix(currentPath)
                remainingPath.substringBefore('/').ifBlank { null } // Get the first segment
            }
            .distinct() // Unique folder names
            .filter { folderName ->
                // Keep folder if query is blank OR if any file *within* this folder path matches the query
                normalizedQuery.isBlank() || filteredFiles.any { it.folderPath.startsWith(currentPath + folderName + "/") }
            }
            .sorted()
            .map { folderName ->
                DisplayItem.FolderItem(name = folderName, path = currentPath + folderName + "/")
            }

        // Find files directly within the current path (using the already filtered list)
        var filesInCurrentPath = filteredFiles
            .filter { it.folderPath == currentPath }

        // Apply sorting to files
        filesInCurrentPath = when (sortCriteria) {
            SortCriteria.ByName -> filesInCurrentPath.sortedBy { it.name.lowercase() } // Sort case-insensitively
            SortCriteria.ByDate -> filesInCurrentPath.sortedBy { it.lastModified }
            SortCriteria.BySize -> filesInCurrentPath.sortedBy { it.size } // Sort by size
        }

        filesInCurrentPath = when (sortOrder) {
            SortOrder.Ascending -> filesInCurrentPath
            SortOrder.Descending -> filesInCurrentPath.reversed()
        }

        val fileItems = filesInCurrentPath.map { DisplayItem.FileItem(it) }

        items.addAll(subFolders)
        items.addAll(fileItems) // Add sorted file items
        return items
    }

    // Functions to update sorting state
    fun setSortCriteria(criteria: SortCriteria) {
        _sortCriteria.value = criteria
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
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
                    // Handle potential FileUriExposedException if file is not accessible
                    try {
                        val uri = FileProvider.getUriForFile(application, authority, file)
                        // Assuming files loaded directly are in root until persistence is added
                        ManagedPdfFile(
                            name = file.name,
                            uri = uri,
                            filePath = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            folderPath = "/", // Assign default path for now
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e(TAG, "Error getting URI for file: ${file.absolutePath}", e)
                        null // Skip file if URI cannot be obtained
                    }
                }?.toList() ?: emptyList() // Removed sorting here, will be handled in calculateDisplayedItems if needed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load PDF files", e)
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
            Log.w(TAG, "Invalid folder path format attempted: $folderPath")
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
            _error.value = null // Clear previous errors
            try {
                val file = File(fileToDelete.filePath)
                if (file.exists()) {
                    Log.i(TAG, "Attempting to delete file: ${fileToDelete.filePath}")
                    if (file.delete()) {
                        Log.i(TAG, "Successfully deleted file: ${fileToDelete.filePath}")
                        // Refresh the list by removing from _allPdfFiles
                        _allPdfFiles.value = _allPdfFiles.value.filterNot { it.filePath == fileToDelete.filePath }
                        // displayedItems will update automatically via the combine flow
                    } else {
                        Log.w(TAG, "System returned false for delete operation on: ${fileToDelete.filePath}")
                        _error.value = "Could not delete file: ${fileToDelete.name}. Operation failed."
                    }
                } else {
                    Log.w(TAG, "File to delete not found: ${fileToDelete.filePath}")
                    _error.value = "File not found: ${fileToDelete.name}. It might have been already deleted."
                    // Also remove from list if it's somehow there but doesn't exist physically
                    _allPdfFiles.value = _allPdfFiles.value.filterNot { it.filePath == fileToDelete.filePath }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException deleting file ${fileToDelete.filePath}", e)
                _error.value = "Permission denied while deleting file: ${fileToDelete.name}"
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting file ${fileToDelete.filePath}", e)
                _error.value = "Error deleting file ${fileToDelete.name}: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // Update renamePdfFile to modify _allPdfFiles
    fun renamePdfFile(fileToRename: ManagedPdfFile, newName: String) {
        viewModelScope.launch {
            _error.value = null // Clear previous errors
            val trimmedNewName = newName.trim() // Trim whitespace
            if (!trimmedNewName.endsWith(".pdf", ignoreCase = true)) {
                _error.value = "New name must end with .pdf"
                return@launch
            }
            // Ensure newName doesn't contain path separators for logical paths
            if (trimmedNewName.isBlank() || trimmedNewName.contains('/')) {
                _error.value = "Invalid file name. Cannot be blank or contain '/'."
                return@launch
            }

            val oldFile = File(fileToRename.filePath)
            val parentDir = oldFile.parentFile
            if (parentDir == null) {
                _error.value = "Cannot determine parent directory for physical file."
                return@launch
            }
            val newPhysicalFile = File(parentDir, trimmedNewName) // Physical file rename

            // Check for physical file collision (case-insensitive on some systems, check explicitly)
            if (newPhysicalFile.exists() && newPhysicalFile.canonicalPath != oldFile.canonicalPath) {
                _error.value = "A physical file with the name '$trimmedNewName' already exists."
                return@launch
            }

            // Check for logical file collision within the same folderPath
            val collisionExists = _allPdfFiles.value.any {
                it.folderPath == fileToRename.folderPath && it.name.equals(trimmedNewName, ignoreCase = true) && it.filePath != fileToRename.filePath
            }
            if (collisionExists) {
                _error.value = "A file with the name '$trimmedNewName' already exists in this folder."
                return@launch
            }

            try {
                Log.i(TAG, "Attempting to rename '${fileToRename.filePath}' to '${newPhysicalFile.absolutePath}'")
                if (oldFile.renameTo(newPhysicalFile)) {
                    Log.i(TAG, "Successfully renamed file.")
                    // Update the specific item in _allPdfFiles
                    _allPdfFiles.value = _allPdfFiles.value.map {
                        if (it.filePath == fileToRename.filePath) {
                            // Update name, physical path, URI and potentially lastModified/size if needed
                            val newUri = FileProvider.getUriForFile(application, "${application.packageName}.provider", newPhysicalFile)
                            it.copy(
                                name = trimmedNewName, // Use trimmed name
                                filePath = newPhysicalFile.absolutePath,
                                uri = newUri,
                                size = newPhysicalFile.length(),
                                lastModified = newPhysicalFile.lastModified(),
                            )
                        } else {
                            it
                        }
                    }
                    // displayedItems will update automatically
                } else {
                    Log.w(TAG, "System returned false for rename operation on: ${fileToRename.filePath}")
                    _error.value = "Could not rename file: ${fileToRename.name}. Operation failed."
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException renaming file ${fileToRename.filePath}", e)
                _error.value = "Permission denied while renaming file: ${e.message}"
            } catch (e: Exception) {
                Log.e(TAG, "Exception renaming file ${fileToRename.filePath}", e)
                _error.value = "Error renaming file ${fileToRename.name}: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    // Temporary function for creating a folder (validation only, no persistence)
    fun createFolder(parentPath: String, newFolderName: String) {
        _error.value = null // Clear previous errors
        val trimmedFolderName = newFolderName.trim()

        // Validate name
        if (trimmedFolderName.isBlank() || trimmedFolderName.contains('/')) {
            _error.value = "Invalid folder name. Cannot be blank or contain '/'."
            return
        }

        // Check for collision in the current displayed items for the parent path
        // Note: This assumes calculateDisplayedItems is up-to-date for the parentPath
        // A more robust check might involve directly querying _allPdfFiles based on parentPath
        val collisionExists = _displayedItems.value.any { item ->
            when (item) {
                is DisplayItem.FolderItem -> item.name.equals(trimmedFolderName, ignoreCase = true)
                is DisplayItem.FileItem -> item.file.name.equals(trimmedFolderName, ignoreCase = true) && item.file.folderPath == parentPath // Check files only in the target parent path
            }
        }

        if (collisionExists) {
            _error.value = "A folder or file with the name '$trimmedFolderName' already exists here."
            return
        }

        // --- Temporary Action ---
        // Since folders are derived from file paths, we don't add anything to _allPdfFiles yet.
        // The folder will appear once a file is moved into its path.
        // For now, just log success or potentially update a transient state if needed for UI feedback.
        println("Temporary createFolder validation successful for: $parentPath$trimmedFolderName/")
        // You could set a temporary success message via another StateFlow if needed.
        // --- End Temporary Action ---

        // TODO: Replace temporary action with actual persistence logic (e.g., DataStore update or placeholder creation)
        // TODO: Implement moveFile function to actually put files into the new folder path.
    }

    // Function to update search query
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // TODO: Implement moveFile which will require data persistence logic
}
