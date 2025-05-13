package com.insnaejack.pdfgenerator.ui.screens.pdflist

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.insnaejack.pdfgenerator.R // Assuming you have string resources
import com.insnaejack.pdfgenerator.model.ManagedPdfFile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    viewModel: PdfListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit, // Example for navigation
) {
    val pdfFiles by viewModel.pdfFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf<ManagedPdfFile?>(null) }
    var newFileName by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // Optionally clear the error in ViewModel after showing
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.pdf_list_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button_desc),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (isLoading && pdfFiles.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (pdfFiles.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.pdf_list_empty), // Replace with actual string resource
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 18.sp,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(pdfFiles, key = { it.filePath }) { pdfFile ->
                        PdfFileItem(
                            pdfFile = pdfFile,
                            onDelete = { viewModel.deletePdfFile(it) },
                            onRename = {
                                showRenameDialog = it
                                newFileName = it.name
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, pdfFile.uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_pdf_title)))
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, context.getString(R.string.share_no_app_found), Toast.LENGTH_SHORT).show()
                                }
                            },
                            onView = {
                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(pdfFile.uri, "application/pdf")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(viewIntent)
                                } catch (e: ActivityNotFoundException) {
                                    Toast.makeText(context, context.getString(R.string.view_no_app_found), Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }
                }
            }

            if (showRenameDialog != null) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = null },
                    title = { Text(stringResource(id = R.string.rename_pdf_dialog_title)) },
                    text = {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text(stringResource(id = R.string.rename_pdf_new_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showRenameDialog?.let { fileToRename ->
                                viewModel.renamePdfFile(fileToRename, newFileName)
                            }
                            showRenameDialog = null
                        }) {
                            Text(stringResource(id = R.string.rename_button))
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showRenameDialog = null }) {
                            Text(stringResource(id = R.string.cancel_button))
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun PdfFileItem(
    pdfFile: ManagedPdfFile,
    onDelete: (ManagedPdfFile) -> Unit,
    onRename: (ManagedPdfFile) -> Unit,
    onShare: (ManagedPdfFile) -> Unit,
    onView: (ManagedPdfFile) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onView(pdfFile) }, // Click card to view
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pdfFile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Size: ${android.text.format.Formatter.formatShortFileSize(context, pdfFile.size)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Last Modified: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            Locale.getDefault(),
                        ).format(Date(pdfFile.lastModified))
                    }",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(id = R.string.more_options_desc))
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.share_action)) },
                        onClick = {
                            onShare(pdfFile)
                            expanded = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.rename_action)) },
                        onClick = {
                            onRename(pdfFile)
                            expanded = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.delete_action)) },
                        onClick = {
                            onDelete(pdfFile)
                            expanded = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
}
