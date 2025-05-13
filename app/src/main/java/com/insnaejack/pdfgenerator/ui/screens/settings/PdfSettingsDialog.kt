package com.insnaejack.pdfgenerator.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.insnaejack.pdfgenerator.R
import com.insnaejack.pdfgenerator.model.ImageQuality
import com.insnaejack.pdfgenerator.model.PageOrientation
import com.insnaejack.pdfgenerator.model.PageSize
import com.insnaejack.pdfgenerator.model.PdfSettings

@Composable
fun PdfSettingsDialog(
    currentSettings: PdfSettings,
    isPremiumUser: Boolean,
    onDismissRequest: () -> Unit,
    onSettingsChanged: (PdfSettings) -> Unit,
) {
    var tempSettings by remember { mutableStateOf(currentSettings) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.pdf_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                if (!isPremiumUser) {
                    Text(
                        text = "Upgrade to Premium to unlock these settings.", // TODO: Add to strings.xml
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }

                // Page Size Dropdown
                PageSizeSelector(
                    label = stringResource(R.string.page_size),
                    selected = tempSettings.pageSize,
                    onSelected = { tempSettings = tempSettings.copy(pageSize = it) },
                    enabled = isPremiumUser,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Orientation Dropdown
                OrientationSelector(
                    label = stringResource(R.string.orientation),
                    selected = tempSettings.orientation,
                    onSelected = { tempSettings = tempSettings.copy(orientation = it) },
                    enabled = isPremiumUser,
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Image Quality Dropdown
                ImageQualitySelector(
                    label = stringResource(R.string.compression_quality),
                    selected = tempSettings.imageQuality,
                    onSelected = { tempSettings = tempSettings.copy(imageQuality = it) },
                    enabled = isPremiumUser,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Margins - For simplicity, not adding UI for margins now, but can be extended.

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSettingsChanged(tempSettings)
                            onDismissRequest()
                        },
                        enabled = isPremiumUser, // Only allow saving if premium, or save but don't apply if not
                    ) {
                        Text(stringResource(R.string.save_settings))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSelector(
    label: String,
    selectedValue: T,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedValue.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown",
                    Modifier.clickable(enabled = enabled) { expanded = true },
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun PageSizeSelector(label: String, selected: PageSize, onSelected: (PageSize) -> Unit, enabled: Boolean) {
    DropdownSelector(
        label = label,
        selectedValue = selected,
        options = PageSize.entries,
        onOptionSelected = onSelected,
        enabled = enabled,
    )
}

@Composable
fun OrientationSelector(
    label: String,
    selected: PageOrientation,
    onSelected: (PageOrientation) -> Unit,
    enabled: Boolean,
) {
    DropdownSelector(
        label = label,
        selectedValue = selected,
        options = PageOrientation.entries,
        onOptionSelected = onSelected,
        enabled = enabled,
    )
}

@Composable
fun ImageQualitySelector(label: String, selected: ImageQuality, onSelected: (ImageQuality) -> Unit, enabled: Boolean) {
    DropdownSelector(
        label = label,
        selectedValue = selected,
        options = ImageQuality.entries,
        onOptionSelected = onSelected,
        enabled = enabled,
    )
}
