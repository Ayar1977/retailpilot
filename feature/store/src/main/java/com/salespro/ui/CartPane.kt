package com.salespro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salespro.data.SaleLine
import java.text.NumberFormat

@Composable
fun CartPane(
    cartLines: List<SaleLine>,
    onUpdateQuantity: (SaleLine, Long) -> Unit,
    onCheckout: () -> Unit,
    isCheckoutEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        currency = java.util.Currency.getInstance("INR")
        maximumFractionDigits = 2
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Active Cart",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (cartLines.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(
                    items = cartLines,
                    key = { it.id }
                ) { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.productNameSnapshot, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                currencyFormat.format(line.unitPriceMinor / 100.0),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Stepper
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = {
                                if (line.quantityBaseUnits > 0) onUpdateQuantity(line, line.quantityBaseUnits - 1)
                            }) { Text("-") }

                            Text("${line.quantityBaseUnits}", modifier = Modifier.padding(horizontal = 8.dp))

                            TextButton(onClick = { onUpdateQuantity(line, line.quantityBaseUnits + 1) }) { Text("+") }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val totalAmount = cartLines.sumOf { it.lineTotalMinor } / 100.0
        Button(
            onClick = onCheckout,
            enabled = isCheckoutEnabled && cartLines.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Complete Sale • ${currencyFormat.format(totalAmount)}")
        }
    }
}
