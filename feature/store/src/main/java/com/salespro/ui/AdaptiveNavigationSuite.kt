package com.salespro.ui

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AdaptiveNavigationSuite(
    layoutPolicy: RetailPilotLayoutPolicy,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val navType = when (layoutPolicy.navigationMode) {
        NavigationMode.BOTTOM_BAR -> NavigationSuiteType.NavigationBar
        NavigationMode.NAVIGATION_RAIL -> NavigationSuiteType.NavigationRail
        NavigationMode.PERMANENT_DRAWER -> NavigationSuiteType.NavigationDrawer
    }

    NavigationSuiteScaffold(
        layoutType = navType,
        navigationSuiteItems = {
            item(
                icon = { androidx.compose.material3.Icon(Icons.Default.ShoppingCart, contentDescription = "Sales") },
                label = { androidx.compose.material3.Text("Sales") },
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) }
            )
            item(
                icon = { androidx.compose.material3.Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                label = { androidx.compose.material3.Text("Reports") },
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) }
            )
        },
        content = content
    )
}
