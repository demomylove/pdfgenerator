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
import androidx.compose.material.icons.filled.Check // Added import for check icon
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search // Import Search icon
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort // Added import for sorting
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
import com.insnaejack.pdfgenerator.R
import com.insnaejack.pdfgenerator.model.ManagedPdfFile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfListScreen(
    viewModel: PdfListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    // Observe state properties from ViewModel
    val displayedItems by viewModel.displayedItems.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val sortCriteria by viewModel.sortCriteria.collectAsState() // Observe sort criteria
    val sortOrder by viewModel.sortOrder.collectAsState() // Observe sort order
    val searchQuery by viewModel.searchQuery.collectAsState() // Observe search query

    val context = LocalContext.current

    var showRenameDialog by remember { mutableStateOf<ManagedPdfFile?>(null) }
    var newFileName by remember { mutableStateOf("") }
    // State for Create Folder Dialog
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    // State for Sort Menu
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // Optionally clear the error in ViewModel after showing
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Display current path, maybe shorten it if too long
                title = { Text(currentPath.ifEmpty { "/" }) },
                navigationIcon = {
                    // Show back arrow only if not in root folder
                    if (currentPath != "/") {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.up_folder_desc), // Add new string resource
                            )
                        }
                    } else {
                        // In root folder, use the original back navigation
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_desc),
                            )
                        }
                    }
                },
                actions = {
                    // Sort Button
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(id = R.string.sort_button_desc), // Add string resource
                        )
                    }

                    // Create Folder Button
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(id = R.string.create_folder_desc), // Add string resource
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) { // Use Column to stack Search and List
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(id = R.string.search_label)) }, // Add string resource
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )

            Box( // Keep Box for list content alignment and dialogs
                modifier = Modifier
                    .fillMaxSize(),
                // .padding(paddingValues), // Padding applied to Column now
            ) {
                // Update loading/empty checks based on displayedItems
                when {
                    isLoading && displayedItems.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    displayedItems.isEmpty() && searchQuery.isBlank() -> { // Show empty only if not searching
                        Text(
                            text = stringResource(id = R.string.folder_empty),
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 18.sp,
                        )
                    }
                    displayedItems.isEmpty() && searchQuery.isNotBlank() -> { // Show "no results" if searching
                        Text(
                            text = stringResource(id = R.string.search_no_results), // Add string resource
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 18.sp,
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), // Adjust padding
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Iterate over displayedItems
                            items(displayedItems, key = { item ->
                                when (item) {
                                    is DisplayItem.FolderItem -> item.path // Use path as key for folders
                                    is DisplayItem.FileItem -> item.file.filePath // Use filePath as key for files
                                }
                            }) { item ->
                                when (item) {
                                    is DisplayItem.FolderItem -> {
                                        FolderItemRow(
                                            folderName = item.name,
                                            onClick = { viewModel.navigateToFolder(item.path) },
                                        )
                                    }
                                    is DisplayItem.FileItem -> {
                                        val pdfFile = item.file
                                        PdfFileItem( // Reuse existing PdfFileItem composable
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
                        }
                    }
                } // End when

                // --- Dialogs and Menus anchored within the Box ---

                // Sort Dropdown Menu
                // Wrap in Box for positioning if needed, but usually okay as is
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                ) {
                    // Sort by Name
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.sort_by_name_asc)) },
                        onClick = {
                            viewModel.setSortCriteria(SortCriteria.ByName)
                            viewModel.setSortOrder(SortOrder.Ascending)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortCriteria == SortCriteria.ByName && sortOrder == SortOrder.Ascending) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.sort_by_name_desc)) },
                        onClick = {
                            viewModel.setSortCriteria(SortCriteria.ByName)
                            viewModel.setSortOrder(SortOrder.Descending)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortCriteria == SortCriteria.ByName && sortOrder == SortOrder.Descending) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                    Divider() // Add a visual separator

                    // Sort by Date
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.sort_by_date_asc)) },
                        onClick = {
                            viewModel.setSortCriteria(SortCriteria.ByDate)
                            viewModel.setSortOrder(SortOrder.Ascending)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortCriteria == SortCriteria.ByDate && sortOrder == SortOrder.Ascending) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.sort_by_date_desc)) },
                        onClick = {
                            viewModel.setSortCriteria(SortCriteria.ByDate)
                            viewModel.setSortOrder(SortOrder.Descending)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortCriteria == SortCriteria.ByDate && sortOrder == SortOrder.Descending) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                    Divider() // Add a visual separator

                    // Sort by Size
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.sort_by_size_asc)) },
                        onClick = {
                            viewModel.setSortCriteria(SortCriteria.BySize)
                            viewModel.setSortOrder(SortOrder.Ascending)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortCriteria == SortCriteria.BySize && sortOrder == SortOrder.Ascending) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.sort_by_size_desc)) },
                        onClick = {
                            viewModel.setSortCriteria(SortCriteria.BySize)
                            viewModel.setSortOrder(SortOrder.Descending)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortCriteria == SortCriteria.BySize && sortOrder == SortOrder.Descending) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        },
                    )
                } // End DropdownMenu

                // Rename dialog
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
                } // End Rename Dialog

                // Create Folder Dialog
                if (showCreateFolderDialog) {
                    AlertDialog(
                        onDismissRequest = { showCreateFolderDialog = false },
                        title = { Text(stringResource(id = R.string.create_folder_dialog_title)) },
                        text = {
                            OutlinedTextField(
                                value = newFolderName,
                                onValueChange = { newFolderName = it },
                                label = { Text(stringResource(id = R.string.create_folder_name_label)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newFolderName.isNotBlank() && !newFolderName.contains('/')) { // Basic validation
                                        // TODO: Call viewModel.createFolder(currentPath, newFolderName) when implemented
                                        println("Create folder requested: $newFolderName in path $currentPath") // Placeholder action
                                        showCreateFolderDialog = false
                                        newFolderName = "" // Reset name
                                    } else {
                                        // Show error toast?
                                        Toast.makeText(context, context.getString(R.string.invalid_folder_name), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                // Enable button only if name is not blank and valid
                                enabled = newFolderName.isNotBlank() && !newFolderName.contains('/'),
                            ) {
                                Text(stringResource(id = R.string.create_button))
                            }
                        },
                        dismissButton = {
                            Button(onClick = {
                                showCreateFolderDialog = false
                                newFolderName = "" // Reset name
                            }) {
                                Text(stringResource(id = R.string.cancel_button))
                            }
                        },
                    )
                } // End Create Folder Dialog
            } // End Box
        } // End Column
    } // End Scaffold lambda
} // End PdfListScreen Composable Function

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
            Box { // Use Box for the dropdown anchor
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
                } // End DropdownMenu
            } // End Box for dropdown anchor
        } // End Row
    } // End Card
} // End PdfFileItem

// New Composable for displaying a folder item
@Composable
fun FolderItemRow(
    folderName: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), // Slightly less elevation than files?
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp) // Adjust padding as needed
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = stringResource(id = R.string.folder_icon_desc), // Add string resource
                modifier = Modifier.size(40.dp), // Adjust size
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = folderName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            // Optionally add a > icon or similar at the end
        } // End Row
    } // End Card
} // End FolderItemRow
