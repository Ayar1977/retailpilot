package com.salespro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.salespro.core.R

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.salespro.data.Product
import com.salespro.data.SaleLine

@Composable
fun SalesScreen(
    products: List<Product>,
    cartLines: List<SaleLine>,
    onProductSelected: (Product) -> Unit,
    onUpdateQuantity: (SaleLine, Long) -> Unit,
    onCheckout: () -> Unit,
    isCheckoutEnabled: Boolean,
    modifier: Modifier = Modifier,
    onScan: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        SalesScreenContent(products, cartLines, onProductSelected, onUpdateQuantity, onCheckout, isCheckoutEnabled)

        // Scan a piece (adds 1) or a box (adds its piece count) to the cart
        FloatingActionButton(
            onClick = onScan,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode))
        }
    }
}

@Composable
private fun SalesScreenContent(
    products: List<Product>,
    cartLines: List<SaleLine>,
    onProductSelected: (Product) -> Unit,
    onUpdateQuantity: (SaleLine, Long) -> Unit,
    onCheckout: () -> Unit,
    isCheckoutEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutPolicy = calculateRetailPilotLayoutPolicy(adaptiveInfo)

    if (layoutPolicy.checkoutMode == CheckoutMode.TWO_PANE) {
        SupportingPaneCheckout(
            products = products,
            cartLines = cartLines,
            onProductClick = onProductSelected,
            onUpdateQuantity = onUpdateQuantity,
            onCheckout = onCheckout,
            isCheckoutEnabled = isCheckoutEnabled,
            modifier = modifier
        )
    } else {
        CompactCheckout(
            products = products,
            cartLines = cartLines,
            onProductClick = onProductSelected,
            onUpdateQuantity = onUpdateQuantity,
            onCheckout = onCheckout,
            isCheckoutEnabled = isCheckoutEnabled,
            modifier = modifier
        )
    }
}
