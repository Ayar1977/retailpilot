package com.salespro.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.salespro.data.Product
import com.salespro.data.SaleLine

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SupportingPaneCheckout(
    products: List<Product>,
    cartLines: List<SaleLine>,
    onProductClick: (Product) -> Unit,
    onUpdateQuantity: (SaleLine, Long) -> Unit,
    onCheckout: () -> Unit,
    isCheckoutEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    // NavigableSupportingPaneScaffold helps adapt the layout and transitions between panes
    val navigator = rememberSupportingPaneScaffoldNavigator()

    // Optionally drive pane visibility based on cart emptiness, but in a POS system, the cart is permanently visible on large screens
    LaunchedEffect(cartLines.isEmpty()) {
        if (!cartLines.isEmpty()) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
        }
    }

    NavigableSupportingPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        mainPane = {
            AnimatedPane {
                ProductCatalogue(
                    products = products,
                    cartLines = cartLines,
                    onProductClick = onProductClick,
                    onUpdateQuantity = onUpdateQuantity
                )
            }
        },
        supportingPane = {
            AnimatedPane {
                CartPane(
                    cartLines = cartLines,
                    onUpdateQuantity = onUpdateQuantity,
                    onCheckout = onCheckout,
                    isCheckoutEnabled = isCheckoutEnabled
                )
            }
        }
    )
}
