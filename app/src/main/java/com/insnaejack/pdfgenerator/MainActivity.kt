package com.insnaejack.pdfgenerator // Make sure this matches your applicationId or namespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.insnaejack.pdfgenerator.ui.theme.PdfGeneratorTheme // We'll create this theme file next
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // For Hilt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PdfGeneratorTheme { // Apply our Material You theme
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Set up the navigation host
                    com.insnaejack.pdfgenerator.ui.navigation.AppNavigation()
                }
            }
        }
    }
}

// Preview for the MainActivity content (which is now the NavHost)
// It's often better to preview individual screens.
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PdfGeneratorTheme {
        com.insnaejack.pdfgenerator.ui.navigation.AppNavigation()
    }
}