package com.salespro.ui

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
    onReceiveStock: (productId: Long, quantity: Long, costMinor: Long, sellingMinor: Long, batchNumber: String?) -> Unit
) {
    var showReceiveDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showRemindersSheet by remember { mutableStateOf(false) }

    val filtered = products.filter { it.name.contains(searchText, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            val totalAlerts = activeAlerts.size + pendingReminders.size
            if (totalAlerts > 0) {
                BadgedBox(
                    badge = { Badge { Text(totalAlerts.toString()) } }
                ) {
                    IconButton(onClick = { showRemindersSheet = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Reminders")
                    }
                }
            } else {
                IconButton(onClick = { showRemindersSheet = true }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminders")
                }
            }
        }

        // Search
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text("Search products") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Product list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                    text = "Stock Reminders",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (pendingReminders.isEmpty()) {
                    Text("No pending reminders.", modifier = Modifier.padding(bottom = 32.dp))
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
                                            text = product?.name ?: "Unknown Product",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Reminder: ${reminder.reminderType.name}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    TextButton(onClick = { onDismissReminder(reminder.id) }) {
                                        Text("Dismiss")
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

                val costDisplay = "Cost: ₹${product.costPriceMinor / 100}.${String.format("%02d", product.costPriceMinor % 100)}"
                val activeSellingPrice = product.activeSellingPriceMinor
                val sellingDisplay = if (activeSellingPrice != null && activeSellingPrice > 0) {
                    "Selling: ₹${activeSellingPrice / 100}.${String.format("%02d", activeSellingPrice % 100)}"
                } else {
                    "Selling: Not set"
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
                Icon(Icons.Default.Add, contentDescription = "Receive stock")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Receive")
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
                    text = "Receive Stock",
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
                    label = { Text("Quantity Received") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Purchase Cost per unit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Selling Price per unit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Batch Number (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
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
                        Text("Receive Stock")
                    }
                }
            }
        }
    }
}
