package com.insnaejack.pdfgenerator.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.insnaejack.pdfgenerator.ui.screens.googledrive.GoogleDriveScreen
import com.insnaejack.pdfgenerator.ui.screens.mainscreen.MainScreen
import com.insnaejack.pdfgenerator.ui.screens.mainscreen.MainScreenViewModel

// Define navigation routes
object AppDestinations {
    const val MAIN_SCREEN_ROUTE = "main"
    const val GOOGLE_DRIVE_ROUTE = "google_drive"
    // Add other destinations here as we build them
    // const val IMAGE_PREVIEW_ROUTE = "image_preview"
    // const val PDF_SETTINGS_ROUTE = "pdf_settings"
    // const val PREMIUM_UPGRADE_ROUTE = "premium_upgrade"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestinations.MAIN_SCREEN_ROUTE,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(AppDestinations.MAIN_SCREEN_ROUTE) { backStackEntry ->
            val mainViewModel: MainScreenViewModel = hiltViewModel(backStackEntry)
            // Observe result from GoogleDriveScreen
            val driveResult = backStackEntry.savedStateHandle.getLiveData<List<String>>("drive_selected_uris").value
            LaunchedEffect(driveResult) {
                driveResult?.let { uriStrings ->
                    val uris = uriStrings.mapNotNull { Uri.parse(it) }
                    if (uris.isNotEmpty()) {
                        mainViewModel.addSelectedImageUris(uris)
                    }
                    // Clear the result from SavedStateHandle
                    backStackEntry.savedStateHandle.remove<List<String>>("drive_selected_uris")
                }
            }
            MainScreen(navController = navController, viewModel = mainViewModel)
        }
        composable(AppDestinations.GOOGLE_DRIVE_ROUTE) {
            GoogleDriveScreen(navController = navController) { downloadedUris ->
                // Pass the result back to the previous screen (MainScreen)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("drive_selected_uris", downloadedUris.map { it.toString() }) // Pass URIs as strings
                // Pop back stack is handled within GoogleDriveScreen after onFilesSelected is called
                // navController.popBackStack()
            }
        }
        // Add other composable screens here
        /*
        composable(AppDestinations.IMAGE_PREVIEW_ROUTE) {
            // ImagePreviewScreen(navController = navController)
        }
        composable(AppDestinations.PDF_SETTINGS_ROUTE) {
            // PdfSettingsScreen(navController = navController)
        }
         */
    }
}
