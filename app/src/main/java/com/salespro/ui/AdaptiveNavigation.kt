package com.salespro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

enum class RetailDestination(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Sell("Sell", Icons.Filled.PointOfSale, Icons.Outlined.PointOfSale),
    Inventory("Inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    Purchases("Purchases", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    Reports("Reports", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    More("More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
}

/**
 * Adaptive navigation using NavigationSuiteScaffold.
 * Automatically switches between bottom bar, rail, and drawer
 * based on WindowAdaptiveInfo (width, height, posture).
 */
@Composable
fun AdaptiveNavigation(
    content: @Composable (String) -> Unit
) {
    var selectedDestination by rememberSaveable { mutableStateOf(RetailDestination.Sell.name) }
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()

    // Let NavigationSuiteScaffold decide the correct navigation component
    val layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(windowAdaptiveInfo)

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            RetailDestination.entries.forEach { destination ->
                val isSelected = selectedDestination == destination.name
                item(
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = isSelected,
                    onClick = { selectedDestination = destination.name }
                )
            }
        }
    ) {
        content(selectedDestination)
    }
}
