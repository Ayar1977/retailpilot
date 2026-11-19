package com.salespro.ui

import androidx.compose.ui.res.stringResource
import com.salespro.core.R
import androidx.compose.material.icons.filled.QrCodeScanner
import com.salespro.settings.Money

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.salespro.data.Product

import com.salespro.data.StockAlertEvent
import com.salespro.data.StockReminder
import androidx.compose.material.icons.filled.Notifications

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    products: List<Product>,
    activeAlerts: List<StockAlertEvent> = emptyList(),
    pendingReminders: List<StockReminder> = emptyList(),
    onDismissReminder: (String) -> Unit = {},
    onAddProduct: () -> Unit = {},
    onScanBarcode: () -> Unit = {},
    onReceiveStock: (productId: Long, quantity: Long, costMinor: Long, sellingMinor: Long, batchNumber: String?) -> Unit
) {
    var showReceiveDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showRemindersSheet by remember { mutableStateOf(false) }

    val filtered = products.filter { it.name.contains(searchText, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_inventory),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onScanBarcode) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode))
            }

            val totalAlerts = activeAlerts.size + pendingReminders.size
            if (totalAlerts > 0) {
                BadgedBox(
                    badge = { Badge { Text(totalAlerts.toString()) } }
                ) {
                    IconButton(onClick = { showRemindersSheet = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.reminders))
                    }
                }
            } else {
                IconButton(onClick = { showRemindersSheet = true }) {
                    Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.reminders))
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text(stringResource(R.string.search_products)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (products.isEmpty()) {
            Text(
                text = stringResource(R.string.inventory_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        }

        // Product list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { product ->
                InventoryProductCard(
                    product = product,
                    onReceiveStock = {
                        selectedProduct = product
                        showReceiveDialog = true
                    }
                )
            }
        }
    }

    ExtendedFloatingActionButton(
        onClick = onAddProduct,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text(stringResource(R.string.new_product)) },
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    )
    }

    if (showReceiveDialog && selectedProduct != null) {
        ReceiveStockDialog(
            product = selectedProduct!!,
            onDismiss = {
                showReceiveDialog = false
                selectedProduct = null
            },
            onConfirm = { qty, costMinor, sellingMinor, batchNumber ->
                onReceiveStock(selectedProduct!!.id, qty, costMinor, sellingMinor, batchNumber)
                showReceiveDialog = false
                selectedProduct = null
            }
        )
    }

    if (showRemindersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRemindersSheet = false }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = stringResource(R.string.stock_reminders),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (pendingReminders.isEmpty()) {
                    Text(stringResource(R.string.no_pending_reminders), modifier = Modifier.padding(bottom = 32.dp))
                } else {
                    LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
                        items(pendingReminders) { reminder ->
                            val product = products.find { it.id == reminder.productId }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product?.name ?: stringResource(R.string.unknown_product),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.reminder_type, reminder.reminderType.name),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    TextButton(onClick = { onDismissReminder(reminder.id) }) {
                                        Text(stringResource(R.string.dismiss))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryProductCard(
    product: Product,
    onReceiveStock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                val costDisplay = stringResource(R.string.cost_label, Money.format(product.costPriceMinor))
                val activeSellingPrice = product.activeSellingPriceMinor
                val sellingDisplay = if (activeSellingPrice != null && activeSellingPrice > 0) {
                    stringResource(R.string.selling_label, Money.format(activeSellingPrice))
                } else {
                    stringResource(R.string.selling_not_set)
                }

                Text(
                    text = costDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sellingDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (activeSellingPrice != null && activeSellingPrice > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }

            FilledTonalButton(
                onClick = onReceiveStock,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.receive))
            }
        }
    }
}

@Composable
fun ReceiveStockDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Long, costMinor: Long, sellingMinor: Long, batchNumber: String?) -> Unit
) {
    val initialActivePrice = product.activeSellingPriceMinor

    var quantity by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf(
        if (product.costPriceMinor > 0) (product.costPriceMinor / 100.0).toString() else ""
    ) }
    var sellingPrice by remember { mutableStateOf(
        if (initialActivePrice != null && initialActivePrice > 0)
            (initialActivePrice / 100.0).toString()
        else ""
    ) }
    var batchNumber by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.receive_stock),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.quantity_received)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.purchase_cost_per_unit, Money.symbol())) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.selling_price_per_unit, Money.symbol())) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.batch_number_optional)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantity.toLongOrNull() ?: 0
                            val cost = ((costPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            val sell = ((sellingPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            val batch = batchNumber.takeIf { it.isNotBlank() }
                            if (qty > 0 && sell > 0) {
                                onConfirm(qty, cost, sell, batch)
                            }
                        },
                        enabled = (quantity.toLongOrNull() ?: 0) > 0 && (sellingPrice.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text(stringResource(R.string.receive_stock))
                    }
                }
            }
        }
    }
}
