package com.salespro.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.salespro.data.Product
import com.salespro.data.SaleLine
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import org.junit.Rule
import org.junit.Test

class SalesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productCatalogue_syncsWithCartLines() {
        val products = listOf(
            Product(id = 1L, name = "Test Product A", retailPriceMinor = 10000, barcode = "111", imageUri = "")
        )

        // Use true Compose State to trigger natural recomposition instead of illegal double-setContent
        var cartLinesState by androidx.compose.runtime.mutableStateOf(emptyList<SaleLine>())

        var productClicked = false
        var updateQuantityLine: SaleLine? = null
        var updateQuantityVal = -1L

        composeTestRule.setContent {
            SalesScreen(
                products = products,
                cartLines = cartLinesState,
                onProductSelected = { productClicked = true },
                onUpdateQuantity = { line, qty ->
                    updateQuantityLine = line
                    updateQuantityVal = qty
                },
                onCheckout = {},
                isCheckoutEnabled = true
            )
        }

        // Initially no check circle, no quantity
        composeTestRule.onNodeWithText("Test Product A").assertExists()
        composeTestRule.onNodeWithContentDescription("In cart").assertDoesNotExist()

        // Click adds to cart via parent
        composeTestRule.onNodeWithText("Test Product A").performClick()
        assert(productClicked)

        // Simulate ViewModel updating cartLines state reactively
        val newLine = SaleLine(
            id = "LINE-1", saleId = "SALE-1", productId = 1L,
            productNameSnapshot = "Test Product A", quantityBaseUnits = 1L,
            unitPriceMinor = 10000, unitCostMinor = 8000, lineTotalMinor = 10000, addedAt = 0L
        )
        cartLinesState = listOf(newLine)

        // Wait implicitly via assertions rather than forcing infinite loop Idle waits
        composeTestRule.onNodeWithContentDescription("In cart", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("1", substring = false, useUnmergedTree = true).assertExists() // Stepper text

        // Tap plus on stepper
        composeTestRule.onNodeWithText("+").performClick()
        assert(updateQuantityLine == newLine)
        assert(updateQuantityVal == 2L)

        // Tap minus on stepper
        composeTestRule.onNodeWithText("-").performClick()
        assert(updateQuantityVal == 0L)
    }
}
