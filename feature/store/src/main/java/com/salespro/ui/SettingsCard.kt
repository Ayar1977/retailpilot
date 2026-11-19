package com.salespro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salespro.core.R
import com.salespro.settings.AppLanguage
import com.salespro.settings.AppSettings
import com.salespro.settings.Currencies
import com.salespro.settings.CurrencyOption

/**
 * Language and currency settings. [onSettingsChanged] should recreate the
 * activity so the new language, text direction and currency take effect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(
    onSettingsChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage = remember { AppSettings.language }
    val currentCurrency = remember { AppSettings.currency }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Language
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = language == currentLanguage,
                        onClick = {
                            if (language != currentLanguage) {
                                AppSettings.setLanguage(language)
                                onSettingsChanged()
                            }
                        },
                        label = { Text(languageLabel(language)) }
                    )
                }
            }

            // Currency
            Text(stringResource(R.string.settings_currency), style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = currencyMenuExpanded,
                onExpandedChange = { currencyMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = currencyLabel(currentCurrency),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = currencyMenuExpanded,
                    onDismissRequest = { currencyMenuExpanded = false }
                ) {
                    Currencies.all.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(currencyLabel(option)) },
                            onClick = {
                                currencyMenuExpanded = false
                                if (option.code != currentCurrency.code) {
                                    AppSettings.setCurrency(option.code)
                                    onSettingsChanged()
                                }
                            }
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.settings_currency_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.ENGLISH -> "English"
    AppLanguage.ARABIC -> "العربية"
}

@Composable
private fun currencyLabel(option: CurrencyOption): String =
    "${stringResource(option.nameRes)} (${option.symbol})"
