package com.salespro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.salespro.core.R
import com.salespro.data.Product
import com.salespro.data.ProductPackaging
import com.salespro.settings.Money

/**
 * Lists and adds packs (box/carton barcodes) for one product.
 * [onScanBarcode] opens the camera scanner and delivers the scanned code.
 */
@Composable
fun PackagingDialog(
    product: Product,
    packagings: List<ProductPackaging>,
    onAdd: (name: String, barcode: String, units: Long) -> Unit,
    onDelete: (ProductPackaging) -> Unit,
    onScanBarcode: (onCode: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultName = stringResource(R.string.default_pack_name)
    var name by remember { mutableStateOf(defaultName) }
    var barcode by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.packs_title, product.name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.packs_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Existing packs
                if (packagings.isEmpty()) {
                    Text(stringResource(R.string.no_packs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    packagings.forEach { pack ->
                        val unitPrice = product.activeSellingPriceMinor ?: 0L
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(
                                        R.string.pack_row,
                                        pack.name,
                                        pack.unitsPerPack,
                                        Money.format(unitPrice * pack.unitsPerPack)
                                    ),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    pack.barcode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onDelete(pack) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // New pack form
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.pack_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(stringResource(R.string.pack_barcode)) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { onScanBarcode { code -> barcode = code } }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = units,
                    onValueChange = { units = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.pack_units)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(
                        onClick = {
                            onAdd(name.trim().ifBlank { defaultName }, barcode.trim(), units.toLongOrNull() ?: 0L)
                            barcode = ""
                            units = ""
                        },
                        enabled = barcode.isNotBlank() && (units.toLongOrNull() ?: 0L) >= 2L
                    ) {
                        Text(stringResource(R.string.add_pack))
                    }
                }
            }
        }
    }
}
