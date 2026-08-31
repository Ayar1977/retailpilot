package com.salespro.data

/**
 * Typed domain result for sale completion.
 * DAO may throw internally to trigger Room rollback,
 * but Repository maps exceptions to these results.
 */
sealed class SaleResult {
    data class Success(val saleId: String, val totalMinor: Long) : SaleResult()
    data class InsufficientStock(val productName: String, val available: Long, val requested: Long) : SaleResult()
    data class MissingSellingPrice(val productName: String) : SaleResult()
    data class InvalidQuantity(val productName: String) : SaleResult()
    object EmptyCart : SaleResult()
    object InvalidPayment : SaleResult()
    object AlreadyCompleted : SaleResult()
    data class DatabaseFailure(val message: String) : SaleResult()
}

/**
 * Typed domain result for add-to-cart.
 */
sealed class CartResult {
    object Success : CartResult()
    data class InsufficientStock(val productName: String, val available: Long) : CartResult()
    data class MissingSellingPrice(val productName: String) : CartResult()
    data class ProductNotFound(val productId: Long) : CartResult()
}

/**
 * Internal typed exceptions used inside DAO @Transaction to force rollback.
 * These MUST be caught at the repository boundary.
 */
sealed class SaleDomainException(message: String) : Exception(message) {
    class InsufficientStockException(val productName: String, val available: Long, val requested: Long)
        : SaleDomainException("Insufficient stock for $productName (available=$available, requested=$requested)")
    class MissingPriceException(val productName: String)
        : SaleDomainException("No selling price set for $productName")
    class InvalidQuantityException(val productName: String)
        : SaleDomainException("Invalid quantity for $productName")
    class EmptyCartException
        : SaleDomainException("Cannot complete an empty cart")
    class AlreadyCompletedException
        : SaleDomainException("Sale is already completed")
}
