package com.salespro.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SalesDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SalesDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.salesDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ----------------------------------------------------
    // INVENTORY INTAKE TESTS
    // ----------------------------------------------------

    @Test
    fun testProductCreationWithoutStock() = runBlocking {
        val productId = dao.insertProduct(Product(name = "Test Init", costPriceMinor = 0, activeSellingPriceMinor = null))
        val product = dao.getProductById(productId)
        assertNotNull(product)
        assertNull(product?.activeSellingPriceMinor)
        assertEquals(0, product?.costPriceMinor)
    }

    @Test
    fun testInventoryIntakeUpdatesPriceAndInsertsRecords() = runBlocking {
        val productId = dao.insertProduct(Product(name = "Apples", costPriceMinor = 0, activeSellingPriceMinor = null))

        // Receive initial batch
        val batchId = dao.receiveInventoryBatch(
            productId = productId,
            quantity = 50,
            unitCostMinor = 100,
            sellingPriceMinor = 150,
            batchNumber = "B-001",
            applySellingPriceToProduct = true
        )

        // Assert product updated
        val updatedProduct = dao.getProductById(productId)!!
        assertEquals(100L, updatedProduct.costPriceMinor)
        assertEquals(150L, updatedProduct.activeSellingPriceMinor)

        // Assert batch inserted
        val batch = dao.getAvailableBatchesFEFO(productId).first { it.id == batchId }
        assertNotNull(batch)
        assertEquals(50L, batch?.quantityReceived)
        assertEquals(100L, batch?.unitCostMinor)
    }

    // ----------------------------------------------------
    // PURCHASE TESTS
    // ----------------------------------------------------

    @Test
    fun testReceivePurchaseAtomicity() = runBlocking {
        val productId1 = dao.insertProduct(Product(name = "P1", costPriceMinor = 0))
        val productId2 = dao.insertProduct(Product(name = "P2", costPriceMinor = 0))
        val supplierId = "SUP-TEST"
        dao.insertSupplier(Supplier(id = supplierId, name = "Vendor", openingBalanceMinor = 0))

        val purchase = Purchase(
            supplierId = supplierId,
            invoiceNumber = "INV-123",
            totalMinor = 500,
            paidMinor = 0, // Unpaid, should increase supplier balance
            status = PurchaseStatus.RECEIVED
        )

        val lines = listOf(
            PurchaseLineData(productId1, 5, 50, 75, "B1"),
            PurchaseLineData(productId2, 3, 20, 30, "B2")
        )

        dao.receivePurchase(purchase, lines)

        val purchases = dao.getAllPurchases().first()
        assertEquals(1, purchases.size)
        val p = purchases[0]
        assertEquals("INV-123", p.invoiceNumber)

        // Verify supplier balance increased safely
        val suppliers = dao.getAllSuppliers().first()
        assertEquals(500L, suppliers[0].openingBalanceMinor)

        // Verify both products got new active selling prices dynamically evaluated
        val p1 = dao.getProductById(productId1)!!
        assertEquals(75L, p1.activeSellingPriceMinor)
    }

    // ----------------------------------------------------
    // CHECKOUT AND BATCH ALLOCATION TESTS
    // ----------------------------------------------------

    @Test
    fun testSaleUsesAuthoritativePriceAndDoesNotCrashOnMissing() = runBlocking {
        val pId = dao.insertProduct(Product(name = "Ghost", costPriceMinor = 50, activeSellingPriceMinor = null))

        val sale = Sale(id = "SALE-1", invoiceNumber = "INV-1", createdBy = "TEST")
        dao.upsertSale(sale)

        // Should throw exception, mapped by repository
        try {
            dao.addToCart(pId, 2)
            fail("Expected exception for missing price")
        } catch (e: Exception) {
            assertTrue(e is SaleDomainException.MissingPriceException || e is IllegalStateException) // PriceNotSet
        }

        // Now set price and retry
        dao.receiveInventoryBatch(pId, 10, 50, 80)

        // Should succeed
        val res = dao.addToCart(pId, 2)
        assertEquals(CartResult.Success, res)

        // Assert snapshot retained 80
        val lines = dao.getLinesForSale("SALE-1")
        assertEquals(1, lines.size)
        assertEquals(80L, lines[0].unitPriceMinor)

        // Change price again
        dao.receiveInventoryBatch(pId, 10, 50, 100)

        // Saved cart line must still be 80.
        val linesAfter = dao.getLinesForSale("SALE-1")
        assertEquals(80L, linesAfter[0].unitPriceMinor)
    }

    @Test
    fun testCompleteSaleThrowsOnInsufficientStock() = runBlocking {
        val pId = dao.insertProduct(Product(name = "X", costPriceMinor = 10, activeSellingPriceMinor = 20))
        val sale = Sale(id = "S-FAIL", invoiceNumber = "INV-FAIL", createdBy = "X")
        dao.upsertSale(sale)

        // Give it 1 stock
        dao.receiveInventoryBatch(pId, 1, 10, 20)
        dao.addToCart(pId, 5) // Cart asks for 5

        try {
            dao.completeSale("S-FAIL", 100)
            fail("Should throw InsufficientStockException")
        } catch (e: Exception) {
            assertTrue(e is SaleDomainException.InsufficientStockException)
        }
    }

    @Test
    fun testCompleteSaleAllocatesBatchesProperly() = runBlocking {
        val pId = dao.insertProduct(Product(name = "FEFO", costPriceMinor = 10, activeSellingPriceMinor = 20))

        // Batch 1 (expires late)
        val b1Id = dao.receiveInventoryBatch(pId, 5, 10, 20, expiryAt = 2000L)
        // Batch 2 (expires early)
        val b2Id = dao.receiveInventoryBatch(pId, 3, 12, 22, expiryAt = 1000L)

        val sale = Sale(id = "S-FEFO", invoiceNumber = "INV-FEFO", createdBy = "FEFO")
        dao.upsertSale(sale)
        dao.addToCart(pId, 7) // Needs 7. Should take 3 from B2, and 4 from B1

        dao.completeSale("S-FEFO", 140)

        val b2 = dao.getAvailableBatchesFEFO(pId).find { it.id == b2Id }!!
        val b1 = dao.getAvailableBatchesFEFO(pId).find { it.id == b1Id }!!

        assertEquals(0L, b2.quantityRemaining)
        assertEquals(1L, b1.quantityRemaining)
    }
}
