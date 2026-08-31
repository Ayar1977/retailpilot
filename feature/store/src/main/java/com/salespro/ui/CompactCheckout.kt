package com.salespro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salespro.data.Product
import com.salespro.data.SaleLine
import java.text.NumberFormat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactCheckout(
    products: List<Product>,
    cartLines: List<SaleLine>,
    onProductClick: (Product) -> Unit,
    onUpdateQuantity: (SaleLine, Long) -> Unit,
    onCheckout: () -> Unit,
    isCheckoutEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var cartSheetVisible by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currencyFormat = NumberFormat.getCurrencyInstance().apply {
        currency = java.util.Currency.getInstance("INR")
        maximumFractionDigits = 2
    }

    val totalAmount = cartLines.sumOf { it.lineTotalMinor } / 100.0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (cartLines.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { cartSheetVisible = true },
                    icon = { Icon(Icons.Default.ShoppingCart, "View Cart") },
                    text = { Text("${cartLines.size} items • ${currencyFormat.format(totalAmount)}") }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        ProductCatalogue(
            products = products,
            cartLines = cartLines,
            onProductClick = onProductClick,
            onUpdateQuantity = onUpdateQuantity,
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (cartSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { cartSheetVisible = false },
            sheetState = sheetState
        ) {
            CartPane(
                cartLines = cartLines,
                onUpdateQuantity = onUpdateQuantity,
                onCheckout = {
                    onCheckout()
                    cartSheetVisible = false
                },
                isCheckoutEnabled = isCheckoutEnabled,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f)
            )
        }
    }
}
