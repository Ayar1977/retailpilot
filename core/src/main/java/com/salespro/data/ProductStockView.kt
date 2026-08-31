package com.salespro.data

import androidx.room.DatabaseView

@DatabaseView(
    viewName = "product_stock_view",
    value = """
        SELECT
            productId,
            COALESCE(SUM(quantityChangeBaseUnits), 0) AS availableBaseUnits
        FROM stock_movements
        GROUP BY productId
    """
)
data class ProductStockView(
    val productId: Long,
    val availableBaseUnits: Long
)
