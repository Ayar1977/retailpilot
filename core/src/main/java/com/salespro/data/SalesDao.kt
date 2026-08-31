package com.salespro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface SalesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReferenceLink(link: ReferenceProductLink)

    @Query("SELECT * FROM reference_links WHERE templateKey = :key")
    suspend fun getReferenceLink(key: String): ReferenceProductLink?

    @Query("UPDATE reference_links SET productId = :productId WHERE templateKey = :key")
    suspend fun updateReferenceLinkProduct(key: String, productId: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseLine(line: PurchaseLine)

    @Query("SELECT * FROM purchases ORDER BY createdAt DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("UPDATE suppliers SET openingBalanceMinor = openingBalanceMinor + :amountMinor WHERE id = :supplierId")
    suspend fun increaseSupplierBalance(supplierId: String, amountMinor: Long)

    @Query("UPDATE products SET costPriceMinor = :costPriceMinor WHERE id = :id")
    suspend fun updateProductPrice(id: Long, costPriceMinor: Long)

    @Query("UPDATE products SET activeSellingPriceMinor = :priceMinor, pricingUpdatedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun updateSellingPrice(id: Long, priceMinor: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    suspend fun getProductByName(name: String): Product?

    @Query("SELECT * FROM sales WHERE saleStatus = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveDraft(): Sale?

    @Query("SELECT * FROM sales WHERE saleStatus = 'DRAFT' ORDER BY updatedAt DESC LIMIT 1")
    fun observeActiveDraft(): Flow<Sale?>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: String): Sale?

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY addedAt ASC, id ASC")
    suspend fun getLinesForSale(saleId: String): List<SaleLine>

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId ORDER BY addedAt ASC, id ASC")
    fun observeCartLines(saleId: String): Flow<List<SaleLine>>

    @Query("UPDATE sale_lines SET quantityBaseUnits = :newQty, lineTotalMinor = unitPriceMinor * :newQty WHERE id = :lineId")
    suspend fun updateLineQuantity(lineId: String, newQty: Long)

    @Query("DELETE FROM sale_lines WHERE id = :lineId")
    suspend fun deleteLine(lineId: String)

    @Query("SELECT * FROM product_stock_view WHERE productId = :productId")
    suspend fun getStockForProduct(productId: Long): ProductStockView?

    @Upsert
    suspend fun upsertSale(sale: Sale)

    @Upsert
    suspend fun upsertSaleLine(line: SaleLine)

    @Insert
    suspend fun insertStockMovement(movement: StockMovement)

    @Insert
    suspend fun insertPayment(payment: Payment)

    @Insert
    suspend fun insertStockBatch(batch: StockBatch)

    @Insert
    suspend fun insertPriceHistory(history: ProductPriceHistory)

    @Insert
    suspend fun insertSaleLineAllocation(allocation: SaleLineAllocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAlertEvent(event: StockAlertEvent)

    @Query("SELECT * FROM stock_alert_events WHERE status = 'ACTIVE'")
    fun getActiveStockAlerts(): Flow<List<StockAlertEvent>>

    @Query("SELECT * FROM stock_alert_events WHERE productId = :productId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveStockAlertForProduct(productId: Long): StockAlertEvent?

    @Query("UPDATE stock_alert_events SET status = :status, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateStockAlertStatus(id: String, status: StockAlertStatus, resolvedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockReminder(reminder: StockReminder)

    @Query("SELECT * FROM stock_reminders WHERE status = 'PENDING'")
    fun getPendingStockReminders(): Flow<List<StockReminder>>

    @Query("UPDATE stock_reminders SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStockReminderStatus(id: String, status: StockReminderStatus, completedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM stock_batches WHERE productId = :productId AND quantityRemaining > 0 AND status = 'ACTIVE' ORDER BY CASE WHEN expiryAt IS NOT NULL THEN expiryAt ELSE 9999999999999 END ASC, receivedAt ASC, id ASC")
    suspend fun getAvailableBatchesFEFO(productId: Long): List<StockBatch>

    @Query("UPDATE stock_batches SET quantityRemaining = :remaining WHERE id = :batchId")
    suspend fun updateBatchRemaining(batchId: String, remaining: Long)

    /**
     * Add to cart: loads authoritative product price inside transaction.
     * Rejects missing selling price. Validates stock for fast feedback.
     */
    @Transaction
    suspend fun addToCart(productId: Long, quantityBaseUnits: Long): CartResult {
        val product = getProductById(productId)
            ?: return CartResult.ProductNotFound(productId)

        val sellingPrice = product.activeSellingPriceMinor
        if (sellingPrice == null || sellingPrice <= 0L) {
            return CartResult.MissingSellingPrice(product.name)
        }

        // Fast stock feedback (not a hard reservation)
        val stock = getStockForProduct(productId)
        val available = stock?.availableBaseUnits ?: 0
        if (available < quantityBaseUnits) {
            return CartResult.InsufficientStock(product.name, available)
        }

        val active = getActiveDraft()
        val sale = active ?: Sale(
            id = "SALE-${UUID.randomUUID()}",
            invoiceNumber = "INV-${System.currentTimeMillis()}",
            subtotalMinor = 0, discountMinor = 0, taxMinor = 0, totalMinor = 0,
            paymentStatus = PaymentStatus.UNPAID, saleStatus = SaleStatus.DRAFT,
            syncStatus = SyncStatus.LOCAL_ONLY, inventoryApplied = false,
            createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()
        )
        if (active == null) {
            upsertSale(sale)
        }

        val existingLines = getLinesForSale(sale.id)
        val existingLine = existingLines.find { it.productId == productId }

        if (existingLine != null) {
            val newQty = existingLine.quantityBaseUnits + quantityBaseUnits
            updateLineQuantity(existingLine.id, newQty)
        } else {
            val newLine = SaleLine(
                id = "LINE-${UUID.randomUUID()}",
                saleId = sale.id,
                productId = product.id,
                productNameSnapshot = product.name,
                barcodeSnapshot = product.barcode.takeIf { it.isNotBlank() },
                quantityBaseUnits = quantityBaseUnits,
                unitPriceMinor = sellingPrice,
                unitCostMinor = product.costPriceMinor,
                discountMinor = 0,
                taxMinor = 0,
                lineTotalMinor = sellingPrice * quantityBaseUnits,
                addedAt = System.currentTimeMillis()
            )
            upsertSaleLine(newLine)
        }

        upsertSale(sale.copy(updatedAt = System.currentTimeMillis()))
        return CartResult.Success
    }

    /**
     * Complete sale: atomic transaction with typed exceptions for rollback.
     * Implements FEFO/FIFO batch allocation.
     * Throws SaleDomainException subtypes to trigger rollback;
     * Repository MUST catch these and map to SaleResult.
     */
    @Transaction
    suspend fun completeSale(saleId: String, receivedAmountMinor: Long) {
        val sale = getSaleById(saleId)
            ?: throw SaleDomainException.AlreadyCompletedException()
        if (sale.saleStatus != SaleStatus.DRAFT) throw SaleDomainException.AlreadyCompletedException()
        if (sale.inventoryApplied) throw SaleDomainException.AlreadyCompletedException()

        val lines = getLinesForSale(saleId)
        if (lines.isEmpty()) throw SaleDomainException.EmptyCartException()

        var calculatedTotal = 0L
        for (line in lines) {
            if (line.quantityBaseUnits <= 0) throw SaleDomainException.InvalidQuantityException(line.productNameSnapshot)
            if (line.productId != null) {
                // Validate stock
                val stock = getStockForProduct(line.productId)
                val available = stock?.availableBaseUnits ?: 0
                if (available < line.quantityBaseUnits) {
                    throw SaleDomainException.InsufficientStockException(
                        line.productNameSnapshot, available, line.quantityBaseUnits
                    )
                }

                // FEFO/FIFO batch allocation
                var remaining = line.quantityBaseUnits
                val batches = getAvailableBatchesFEFO(line.productId)
                for (batch in batches) {
                    if (remaining <= 0) break
                    val take = minOf(remaining, batch.quantityRemaining)
                    updateBatchRemaining(batch.id, batch.quantityRemaining - take)
                    insertSaleLineAllocation(SaleLineAllocation(
                        saleLineId = line.id,
                        batchId = batch.id,
                        quantity = take,
                        unitCostMinor = batch.unitCostMinor
                    ))
                    remaining -= take
                }
                // If batches didn't cover it, fall back to movement-only deduction
                // (legacy products without batches)

                // Track stock deduction via movement ledger
                insertStockMovement(StockMovement(
                    id = "SM-${UUID.randomUUID()}",
                    productId = line.productId,
                    type = StockMovementType.SALE,
                    quantityChangeBaseUnits = -line.quantityBaseUnits,
                    referenceType = "SALE",
                    referenceId = saleId,
                    note = "Sale completed",
                    createdAt = System.currentTimeMillis(),
                    createdBy = sale.createdBy
                ))
            }
            calculatedTotal += line.lineTotalMinor
        }

        if (receivedAmountMinor < 0) throw SaleDomainException.EmptyCartException()

        insertPayment(Payment(
            id = "PAY-${UUID.randomUUID()}",
            saleId = saleId,
            method = PaymentMethod.CASH,
            amountMinor = receivedAmountMinor,
            reference = null,
            status = PaymentStatus.PAID.name,
            receivedAt = System.currentTimeMillis()
        ))

        val payStatus = when {
            receivedAmountMinor >= calculatedTotal -> PaymentStatus.PAID
            receivedAmountMinor > 0 -> PaymentStatus.PARTIALLY_PAID
            else -> PaymentStatus.UNPAID
        }

        upsertSale(sale.copy(
            saleStatus = SaleStatus.COMPLETED,
            paymentStatus = payStatus,
            inventoryApplied = true,
            totalMinor = calculatedTotal,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))
    }

    /**
     * Receive inventory batch transactionally.
     * Creates StockBatch, StockMovement, PriceHistory, and optionally updates activeSellingPriceMinor.
     */
    @Transaction
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
        val product = getProductById(productId) ?: throw IllegalStateException("Product not found")
        val now = System.currentTimeMillis()
        val batchId = "BATCH-${UUID.randomUUID()}"

        // 1. Create batch record
        insertStockBatch(StockBatch(
            id = batchId,
            productId = productId,
            batchNumber = batchNumber,
            receivedAt = now,
            expiryAt = expiryAt,
            quantityReceived = quantity,
            quantityRemaining = quantity,
            unitCostMinor = unitCostMinor,
            intakeSellingPriceMinor = sellingPriceMinor,
            sourceReference = sourceReference
        ))

        // 2. Create stock movement
        insertStockMovement(StockMovement(
            id = "SM-${UUID.randomUUID()}",
            productId = productId,
            type = StockMovementType.PURCHASE_RECEIPT,
            quantityChangeBaseUnits = quantity,
            referenceType = "BATCH",
            referenceId = batchId,
            note = "Batch received: ${batchNumber ?: batchId}",
            createdAt = now
        ))

        // 3. Update product selling price if requested
        if (applySellingPriceToProduct && sellingPriceMinor > 0) {
            val previousPrice = product.activeSellingPriceMinor
            updateSellingPrice(productId, sellingPriceMinor, now)

            // 4. Audit price change
            insertPriceHistory(ProductPriceHistory(
                productId = productId,
                previousPriceMinor = previousPrice,
                newPriceMinor = sellingPriceMinor,
                sourceBatchId = batchId,
                changedAt = now,
                reason = "Inventory intake batch: ${batchNumber ?: batchId}"
            ))
        }

        // 5. Update product cost price
        updateProductPrice(productId, unitCostMinor)
        return batchId
    }

    /**
     * Atomically receives an entire purchase with multiple lines.
     * Creates Purchase, PurchaseLines, StockBatches, StockMovements, PriceHistory,
     * and updates Supplier balance if unpaid.
     */
    @Transaction
    suspend fun receivePurchase(
        purchase: Purchase,
        lines: List<PurchaseLineData>
    ) {
        insertPurchase(purchase)

        for (line in lines) {
            val batchId = receiveInventoryBatch(
                productId = line.productId,
                quantity = line.quantity,
                unitCostMinor = line.unitCostMinor,
                sellingPriceMinor = line.sellingPriceMinor,
                batchNumber = line.batchNumber,
                expiryAt = line.expiryAt,
                applySellingPriceToProduct = line.applySellingPriceToProduct,
                sourceReference = purchase.id
            )

            insertPurchaseLine(PurchaseLine(
                id = "PL-${UUID.randomUUID()}",
                purchaseId = purchase.id,
                productId = line.productId,
                batchId = batchId,
                quantity = line.quantity,
                unitCostMinor = line.unitCostMinor,
                sellingPriceMinor = line.sellingPriceMinor,
                discountMinor = line.discountMinor,
                taxMinor = line.taxMinor
            ))
        }

        if (purchase.supplierId != null) {
            val due = purchase.totalMinor - purchase.paidMinor
            if (due > 0) {
                increaseSupplierBalance(purchase.supplierId, due)
            }
        }
    }

    @Query("DELETE FROM sales WHERE id = :saleId AND saleStatus = 'DRAFT' AND inventoryApplied = 0")
    suspend fun deleteDraftCascade(saleId: String): Int

    suspend fun deleteSaleSafely(sale: Sale) {
        if (sale.saleStatus != SaleStatus.DRAFT || sale.inventoryApplied) {
            throw IllegalStateException("Cannot delete a completed, voided, or inventory-applied sale.")
        }
        deleteDraftCascade(sale.id)
    }

    @Transaction
    suspend fun evaluateStockAlert(productId: Long) {
        val product = getProductById(productId) ?: return
        val stock = getStockForProduct(productId)
        val available = stock?.availableBaseUnits ?: 0
        val threshold = product.lowStockThresholdBaseUnits

        val activeEvent = getActiveStockAlertForProduct(productId)

        if (available < threshold) {
            if (activeEvent == null) {
                // Crossing from above threshold into low stock -> new event
                val eventId = "ALERT-${UUID.randomUUID()}"
                insertStockAlertEvent(com.salespro.data.StockAlertEvent(
                    id = eventId,
                    productId = productId,
                    triggerType = StockAlertTriggerType.LOW_STOCK,
                    thresholdSnapshotBaseUnits = threshold,
                    stockAtOpenBaseUnits = available,
                    openedAt = System.currentTimeMillis(),
                    status = StockAlertStatus.ACTIVE,
                    eventSequence = System.currentTimeMillis()
                ))
            }
        } else {
            if (activeEvent != null) {
                // Restocking above threshold -> resolve event
                updateStockAlertStatus(activeEvent.id, StockAlertStatus.RESOLVED)
            }
        }
    }

    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<Sale>>
}

data class PurchaseLineData(
    val productId: Long,
    val quantity: Long,
    val unitCostMinor: Long,
    val sellingPriceMinor: Long,
    val batchNumber: String? = null,
    val expiryAt: Long? = null,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val applySellingPriceToProduct: Boolean = true
)
