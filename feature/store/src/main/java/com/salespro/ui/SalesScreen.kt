package com.salespro.ui

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
