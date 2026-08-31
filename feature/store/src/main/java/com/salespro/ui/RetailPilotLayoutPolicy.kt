package com.salespro.ui

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RetailPilotLayoutPolicy(
    val navigationMode: NavigationMode,
    val checkoutMode: CheckoutMode,
    val isSeparatingFold: Boolean
)

enum class NavigationMode {
    BOTTOM_BAR,
    NAVIGATION_RAIL,
    PERMANENT_DRAWER
}

enum class CheckoutMode {
    COMPACT,
    TWO_PANE
}

fun calculateRetailPilotLayoutPolicy(
    adaptiveInfo: WindowAdaptiveInfo
): RetailPilotLayoutPolicy {
    val windowSizeClass = adaptiveInfo.windowSizeClass
    val isSeparatingFold = adaptiveInfo.windowPosture.isTabletop // or check hinge bounds

    val navigationMode = when {
        windowSizeClass.windowWidthSizeClass == androidx.window.core.layout.WindowWidthSizeClass.COMPACT -> NavigationMode.BOTTOM_BAR
        windowSizeClass.windowWidthSizeClass == androidx.window.core.layout.WindowWidthSizeClass.MEDIUM -> NavigationMode.NAVIGATION_RAIL
        else -> NavigationMode.PERMANENT_DRAWER
    }

    // Minimums
    val catalogueMin = 320.dp
    val cartMin = 280.dp
    val navWidth = if (navigationMode == NavigationMode.PERMANENT_DRAWER) 240.dp else if (navigationMode == NavigationMode.NAVIGATION_RAIL) 80.dp else 0.dp

    // Safe width logic. In a real system, dp values come from actual BoxWithConstraints or derived width.
    // For adaptive, expanded width > 840dp minus rail (80) = 760dp. It fits both (320+280 = 600dp).
    val isHeightCompact = windowSizeClass.windowHeightSizeClass == androidx.window.core.layout.WindowHeightSizeClass.COMPACT
    val checkoutMode = if (windowSizeClass.windowWidthSizeClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED && !isHeightCompact) {
        CheckoutMode.TWO_PANE
    } else {
        CheckoutMode.COMPACT
    }

    return RetailPilotLayoutPolicy(
        navigationMode = navigationMode,
        checkoutMode = checkoutMode,
        isSeparatingFold = isSeparatingFold
    )
}
