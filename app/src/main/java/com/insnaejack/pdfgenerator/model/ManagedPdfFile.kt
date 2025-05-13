package com.insnaejack.pdfgenerator.model

import android.net.Uri

data class ManagedPdfFile(
    val name: String,
    val uri: Uri, // Content URI, especially if using FileProvider for sharing
    val filePath: String, // Actual file path for internal management
    val size: Long, // Size in bytes
    val lastModified: Long, // Timestamp of last modification
)
