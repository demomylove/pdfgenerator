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
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// Define a constant for the directory where PDFs are stored.
// This should ideally be a more robust solution, perhaps configured elsewhere
// or using app-specific directories.
const val PDF_DIRECTORY_NAME = "GeneratedPDFs"

@HiltViewModel
class PdfListViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {

    private val _pdfFiles = MutableStateFlow<List<ManagedPdfFile>>(emptyList())
    val pdfFiles: StateFlow<List<ManagedPdfFile>> = _pdfFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadPdfFiles()
    }

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
                _pdfFiles.value = files?.mapNotNull { file ->
                    // Ensure the authority matches what's in AndroidManifest.xml for FileProvider
                    val authority = "${application.packageName}.provider"
                    val uri = FileProvider.getUriForFile(application, authority, file)
                    ManagedPdfFile(
                        name = file.name,
                        uri = uri,
                        filePath = file.absolutePath,
                        size = file.length(),
                        lastModified = file.lastModified(),
                    )
                }?.sortedByDescending { it.lastModified } ?: emptyList()
            } catch (e: Exception) {
                _error.value = "Failed to load PDF files: ${e.message}"
                _pdfFiles.value = emptyList() // Clear list on error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePdfFile(fileToDelete: ManagedPdfFile) {
        viewModelScope.launch {
            try {
                val file = File(fileToDelete.filePath)
                if (file.exists()) {
                    if (file.delete()) {
                        // Refresh the list
                        _pdfFiles.value = _pdfFiles.value.filterNot { it.filePath == fileToDelete.filePath }
                    } else {
                        _error.value = "Failed to delete file: ${fileToDelete.name}"
                    }
                } else {
                    _error.value = "File not found: ${fileToDelete.name}"
                    // Also remove from list if it's somehow there but doesn't exist
                    _pdfFiles.value = _pdfFiles.value.filterNot { it.filePath == fileToDelete.filePath }
                }
            } catch (e: Exception) {
                _error.value = "Error deleting file ${fileToDelete.name}: ${e.message}"
            }
        }
    }

    // Placeholder for rename functionality
    fun renamePdfFile(fileToRename: ManagedPdfFile, newName: String) {
        viewModelScope.launch {
            _error.value = null // Clear previous errors
            if (!newName.endsWith(".pdf", ignoreCase = true)) {
                _error.value = "New name must end with .pdf"
                return@launch
            }
            if (newName.isBlank() || newName.contains(File.separatorChar)) {
                _error.value = "Invalid file name."
                return@launch
            }

            val oldFile = File(fileToRename.filePath)
            val parentDir = oldFile.parentFile
            if (parentDir == null) {
                _error.value = "Cannot determine parent directory."
                return@launch
            }
            val newFile = File(parentDir, newName)

            if (newFile.exists()) {
                _error.value = "A file with the name '$newName' already exists."
                return@launch
            }

            try {
                if (oldFile.renameTo(newFile)) {
                    // Refresh the list to reflect the change
                    loadPdfFiles()
                } else {
                    _error.value = "Failed to rename file: ${fileToRename.name}"
                }
            } catch (e: SecurityException) {
                _error.value = "Permission denied while renaming file: ${e.message}"
            } catch (e: Exception) {
                _error.value = "Error renaming file ${fileToRename.name}: ${e.message}"
            }
        }
    }
}
