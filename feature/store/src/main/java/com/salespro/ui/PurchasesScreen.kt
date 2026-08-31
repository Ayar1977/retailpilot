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
import com.salespro.data.Purchase
import com.salespro.data.PurchaseLineData

@Composable
fun PurchasesScreen(
    purchases: List<Purchase>,
    products: List<Product>,
    onReceivePurchase: (Purchase, List<PurchaseLineData>) -> Unit
) {
    var showReceiveDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Purchases & Receiving",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = { showReceiveDialog = true },
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Receive Purchase")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Receive New Purchase")
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(purchases) { purchase ->
                PurchaseHistoryCard(purchase)
            }
        }
    }

    if (showReceiveDialog) {
        ReceivePurchaseDialog(
            products = products,
            onDismiss = { showReceiveDialog = false },
            onConfirm = { purchase, lines ->
                onReceivePurchase(purchase, lines)
                showReceiveDialog = false
            }
        )
    }
}

@Composable
fun PurchaseHistoryCard(purchase: Purchase) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = purchase.invoiceNumber ?: "No Invoice #",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total: ₹${purchase.totalMinor / 100}.${String.format("%02d", purchase.totalMinor % 100)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Status: ${purchase.status}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePurchaseDialog(
    products: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (Purchase, List<PurchaseLineData>) -> Unit
) {
    var invoiceNumber by remember { mutableStateOf("") }

    // Simplification for the demo: Single product line received via simple dialog
    // A robust system would use a multi-select screen.
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var quantity by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Receive Purchase", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("Invoice Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "Select Product",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        products.forEach { prod ->
                            DropdownMenuItem(
                                text = { Text(prod.name) },
                                onClick = {
                                    selectedProduct = prod
                                    costPrice = (prod.costPriceMinor / 100.0).toString()
                                    sellingPrice = if (prod.activeSellingPriceMinor != null && prod.activeSellingPriceMinor!! > 0)
                                        (prod.activeSellingPriceMinor!! / 100.0).toString() else ""
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Unit Cost") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Selling Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    label = { Text("Batch Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantity.toLongOrNull() ?: 0L
                            val cost = ((costPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()
                            val sell = ((sellingPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()

                            if (selectedProduct != null && qty > 0 && sell > 0) {
                                val total = qty * cost
                                val purchase = Purchase(
                                    invoiceNumber = invoiceNumber.takeIf { it.isNotBlank() },
                                    subtotalMinor = total,
                                    totalMinor = total,
                                    paidMinor = total,
                                    status = com.salespro.data.PurchaseStatus.PAID
                                )
                                val line = PurchaseLineData(
                                    productId = selectedProduct!!.id,
                                    quantity = qty,
                                    unitCostMinor = cost,
                                    sellingPriceMinor = sell,
                                    batchNumber = batchNumber.takeIf { it.isNotBlank() },
                                    applySellingPriceToProduct = true
                                )
                                onConfirm(purchase, listOf(line))
                            }
                        },
                        enabled = selectedProduct != null && (quantity.toLongOrNull() ?: 0) > 0 && (sellingPrice.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text("Save & Receive")
                    }
                }
            }
        }
    }
}
