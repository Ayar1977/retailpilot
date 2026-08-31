package com.salespro.data

import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

class SalesRepository(
    private val dao: SalesDao,
    private val profileDao: BusinessProfileDao
) {

    fun getBusinessProfile(): Flow<BusinessProfile?> = profileDao.getProfile()
    suspend fun saveBusinessProfile(profile: BusinessProfile) = profileDao.upsertProfile(profile)
    suspend fun resetBusinessProfile() = profileDao.resetProfile()

    suspend fun setupReferenceProducts() {
        val cokeKey = "COKE_250ML_REFERENCE"
        val existingCokeLink = dao.getReferenceLink(cokeKey)

        if (existingCokeLink == null) {
            val cokeId = dao.insertProduct(
                Product(
                    name = "Coca-Cola 250ml",
                    costPriceMinor = 1500L,
                    activeSellingPriceMinor = 2000L, // Rs 20
                    measurementType = com.salespro.data.MeasurementType.PIECE,
                    measurementScale = 1,
                    packageAmountBaseUnits = 250L,
                    packageMeasurementType = com.salespro.data.MeasurementType.VOLUME,
                    lowStockThresholdBaseUnits = 500L,
                    isActive = true,
                    priceNeedsReview = false,
                    barcode = "",
                    sku = java.util.UUID.randomUUID().toString()
                )
            )
            dao.insertReferenceLink(ReferenceProductLink(templateKey = cokeKey, productId = cokeId))
        }
    }

    suspend fun insertReferenceLink(templateKey: String, productId: Long) {
        dao.insertReferenceLink(ReferenceProductLink(templateKey = templateKey, productId = productId))
    }

    suspend fun insertProduct(product: Product): Long = dao.insertProduct(product)

    suspend fun insertStockMovement(movement: StockMovement) = dao.insertStockMovement(movement)

    suspend fun updateProductPrice(id: Long, costPriceMinor: Long) = dao.updateProductPrice(id, costPriceMinor)

    fun getAllProducts(): Flow<List<Product>> = dao.getAllProducts()

    suspend fun getProductById(id: Long): Product? = dao.getProductById(id)

    suspend fun getProductByBarcode(barcode: String): Product? = dao.getProductByBarcode(barcode)

    suspend fun getProductByName(name: String): Product? = dao.getProductByName(name)

    fun getActiveStockAlerts(): Flow<List<StockAlertEvent>> = dao.getActiveStockAlerts()
    fun getPendingStockReminders(): Flow<List<StockReminder>> = dao.getPendingStockReminders()
    suspend fun markReminderCompleted(id: String) = dao.updateStockReminderStatus(id, StockReminderStatus.COMPLETED)

    // Draft cart reactive queries
    fun observeActiveDraft(): Flow<Sale?> = dao.observeActiveDraft()

    fun observeCartLines(saleId: String): Flow<List<SaleLine>> = dao.observeCartLines(saleId)

    /**
     * Add to cart with authoritative price loading.
     * Returns CartResult - never throws business exceptions.
     */
    suspend fun addToCart(productId: Long, quantityBaseUnits: Long): CartResult {
        return try {
            dao.addToCart(productId, quantityBaseUnits)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CartResult.ProductNotFound(productId)
        }
    }

    /**
     * Complete sale: maps DAO typed exceptions to SaleResult.
     * Never lets business exceptions escape.
     */
    suspend fun completeSale(saleId: String, receivedAmountMinor: Long): SaleResult {
        return try {
            dao.completeSale(saleId, receivedAmountMinor)

            // Re-evaluate stock alerts post-transaction
            val lines = dao.getLinesForSale(saleId)
            for (line in lines) {
                line.productId?.let { evaluateStockAlert(it) }
            }

            val sale = dao.getSaleById(saleId)
            SaleResult.Success(saleId, sale?.totalMinor ?: 0L)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SaleDomainException.InsufficientStockException) {
            SaleResult.InsufficientStock(e.productName, e.available, e.requested)
        } catch (e: SaleDomainException.MissingPriceException) {
            SaleResult.MissingSellingPrice(e.productName)
        } catch (e: SaleDomainException.InvalidQuantityException) {
            SaleResult.InvalidQuantity(e.productName)
        } catch (e: SaleDomainException.EmptyCartException) {
            SaleResult.EmptyCart
        } catch (e: SaleDomainException.AlreadyCompletedException) {
            SaleResult.AlreadyCompleted
        } catch (e: Exception) {
            SaleResult.DatabaseFailure(e.message ?: "Unknown database error")
        }
    }

    /**
     * Receive inventory batch.
     */
    suspend fun receiveInventoryBatch(
        productId: Long,
        quantity: Long,
        unitCostMinor: Long,
        sellingPriceMinor: Long,
        batchNumber: String? = null,
        expiryAt: Long? = null,
        applySellingPriceToProduct: Boolean = true,
        sourceReference: String? = null
    ): String {
        return dao.receiveInventoryBatch(
            productId = productId,
            quantity = quantity,
            unitCostMinor = unitCostMinor,
            sellingPriceMinor = sellingPriceMinor,
            batchNumber = batchNumber,
            expiryAt = expiryAt,
            applySellingPriceToProduct = applySellingPriceToProduct,
            sourceReference = sourceReference
        )
    }

    suspend fun receivePurchase(
        purchase: Purchase,
        lines: List<PurchaseLineData>
    ) {
        dao.receivePurchase(purchase, lines)
        for (line in lines) {
            evaluateStockAlert(line.productId)
        }
    }

    fun getAllPurchases() = dao.getAllPurchases()

    suspend fun deleteSaleSafely(sale: Sale) = dao.deleteSaleSafely(sale)

    suspend fun updateLineQuantity(lineId: String, newQty: Long) = dao.updateLineQuantity(lineId, newQty)

    suspend fun deleteLine(lineId: String) = dao.deleteLine(lineId)

    fun getAllSales(): Flow<List<Sale>> = dao.getAllSales()

    suspend fun evaluateStockAlert(productId: Long) {
        dao.evaluateStockAlert(productId)
    }
}
