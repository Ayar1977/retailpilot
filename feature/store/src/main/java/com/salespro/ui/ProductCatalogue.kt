package com.salespro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.salespro.data.Product
import com.salespro.data.SaleLine

@Composable
fun ProductCatalogue(
    products: List<Product>,
    cartLines: List<SaleLine>,
    onProductClick: (Product) -> Unit,
    onUpdateQuantity: (SaleLine, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cartMap = cartLines.associateBy { it.productId }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(
            items = products,
            key = { it.id }
        ) { product ->
            val cartLine = cartMap[product.id]
            val inCart = cartLine != null && cartLine.quantityBaseUnits > 0
            val quantity = cartLine?.quantityBaseUnits ?: 0L

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .clickable {
                        if (inCart && cartLine != null) {
                            onUpdateQuantity(cartLine, quantity + 1)
                        } else {
                            onProductClick(product)
                        }
                    },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (inCart) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    if (!product.imageUri.isNullOrEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                        ) {
                            coil.compose.AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(product.imageUri)
                                    .crossfade(false)
                                    .error(android.R.drawable.ic_menu_gallery) // Fallback generic icon
                                    .fallback(android.R.drawable.ic_menu_gallery)
                                    .build(),
                                contentDescription = product.name,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant)
                            )
                            if (inCart) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "In cart",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        if (inCart) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "In cart",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp).padding(bottom = 4.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (inCart) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${"%.2f".format((product.activeSellingPriceMinor ?: product.retailPriceMinor ?: product.costPriceMinor) / 100.0)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (inCart) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                    )

                    if (inCart && cartLine != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalIconButton(
                                onClick = { onUpdateQuantity(cartLine, quantity - 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-")
                            }
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            FilledTonalIconButton(
                                onClick = { onUpdateQuantity(cartLine, quantity + 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+")
                            }
                        }
                    }
                }
            }
        }
    }
}
