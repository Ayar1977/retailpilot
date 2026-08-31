package com.salespro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Product::class, Sale::class, SaleLine::class,
        Payment::class, StockMovement::class, BusinessProfile::class,
        StockBatch::class, ProductPriceHistory::class, SaleLineAllocation::class,
        Supplier::class, Purchase::class, PurchaseLine::class,
        StockAlertEvent::class, StockReminder::class, ReferenceProductLink::class
    ],
    views = [ProductStockView::class],
    version = 12,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun salesDao(): SalesDao
    abstract fun businessProfileDao(): BusinessProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sales_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `reference_links` (
                        `templateKey` TEXT NOT NULL,
                        `productId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER,
                        PRIMARY KEY(`templateKey`),
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reference_links_templateKey` ON `reference_links` (`templateKey`)")

                db.execSQL("ALTER TABLE stock_reminders ADD COLUMN intendedLocalDate TEXT")
                db.execSQL("ALTER TABLE stock_reminders ADD COLUMN intendedLocalTime TEXT")
                db.execSQL("ALTER TABLE stock_reminders ADD COLUMN intendedZoneId TEXT")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE products_new (id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL, costPrice REAL NOT NULL, imageName TEXT NOT NULL DEFAULT '')")
                db.execSQL("INSERT INTO products_new (id, name, costPrice, imageName) SELECT id, name, costPrice, '' FROM products")
                db.execSQL("DROP TABLE products")
                db.execSQL("ALTER TABLE products_new RENAME TO products")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN ean TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {}
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE sales ADD COLUMN customerPhone TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN stockQuantity INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales RENAME TO legacy_sales")
                db.execSQL("ALTER TABLE products RENAME TO legacy_products")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `business_profile` (
                        `id` INTEGER NOT NULL, `businessName` TEXT NOT NULL,
                        `ownerName` TEXT, `phoneNumber` TEXT, `address` TEXT,
                        `taxRegistrationNumber` TEXT, `receiptFooter` TEXT, `logoUri` TEXT,
                        `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`)
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `products` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sku` TEXT NOT NULL, `barcode` TEXT NOT NULL,
                        `name` TEXT NOT NULL, `retailPriceMinor` INTEGER,
                        `priceNeedsReview` INTEGER NOT NULL, `costPriceMinor` INTEGER NOT NULL,
                        `measurementType` TEXT NOT NULL, `measurementScale` INTEGER NOT NULL,
                        `imageUri` TEXT NOT NULL, `lowStockThresholdBaseUnits` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sales` (
                        `id` TEXT NOT NULL, `invoiceNumber` TEXT NOT NULL,
                        `customerId` TEXT, `customerPhoneSnapshot` TEXT,
                        `subtotalMinor` INTEGER NOT NULL, `discountMinor` INTEGER NOT NULL,
                        `taxMinor` INTEGER NOT NULL, `totalMinor` INTEGER NOT NULL,
                        `paymentStatus` TEXT NOT NULL, `saleStatus` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL, `inventoryApplied` INTEGER NOT NULL,
                        `migrationSource` TEXT, `createdAt` INTEGER NOT NULL,
                        `completedAt` INTEGER, `createdBy` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_createdAt` ON `sales` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_invoiceNumber` ON `sales` (`invoiceNumber`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_lines` (
                        `id` TEXT NOT NULL, `saleId` TEXT NOT NULL,
                        `productId` INTEGER, `productNameSnapshot` TEXT NOT NULL,
                        `barcodeSnapshot` TEXT, `quantityBaseUnits` INTEGER NOT NULL,
                        `unitPriceMinor` INTEGER NOT NULL, `unitCostMinor` INTEGER NOT NULL,
                        `discountMinor` INTEGER NOT NULL, `taxMinor` INTEGER NOT NULL,
                        `lineTotalMinor` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`id`),
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_lines_saleId` ON `sale_lines` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_lines_productId` ON `sale_lines` (`productId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `payments` (
                        `id` TEXT NOT NULL, `saleId` TEXT NOT NULL,
                        `method` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL,
                        `reference` TEXT, `status` TEXT NOT NULL,
                        `receivedAt` INTEGER NOT NULL, PRIMARY KEY(`id`),
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_saleId` ON `payments` (`saleId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `id` TEXT NOT NULL, `productId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL, `quantityChangeBaseUnits` INTEGER NOT NULL,
                        `referenceType` TEXT, `referenceId` TEXT,
                        `note` TEXT, `createdAt` INTEGER NOT NULL,
                        `createdBy` TEXT, PRIMARY KEY(`id`),
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_referenceId` ON `stock_movements` (`referenceId`)")

                db.execSQL("CREATE VIEW IF NOT EXISTS `product_stock_view` AS SELECT\n" +
                    "            productId,\n" +
                    "            COALESCE(SUM(quantityChangeBaseUnits), 0) AS availableBaseUnits\n" +
                    "        FROM stock_movements\n" +
                    "        GROUP BY productId")

                // Migrate Products using explicit CAST rounding rules to map legacy float constraints safely
                db.execSQL("""
                    INSERT INTO products (
                        id, sku, barcode, name, retailPriceMinor, priceNeedsReview, costPriceMinor,
                        measurementType, measurementScale, imageUri, lowStockThresholdBaseUnits,
                        isActive, createdAt, updatedAt
                    )
                    SELECT
                        id, 'LEGACY-SKU-' || id, ean, name, NULL, 1, CAST(ROUND(costPrice * 100.0) AS INTEGER),
                        'PIECE', 1, imageName, 0, 1, 0, 0
                    FROM legacy_products
                """)

                // Migrate Sales Header
                db.execSQL("""
                    INSERT INTO sales (
                        id, invoiceNumber, customerId, customerPhoneSnapshot, subtotalMinor,
                        discountMinor, taxMinor, totalMinor, paymentStatus, saleStatus, syncStatus,
                        inventoryApplied, migrationSource, createdAt, completedAt, createdBy, updatedAt
                    )
                    SELECT
                        'legacy-sale-v6-' || id, 'LEGACY-' || id, NULL, customerPhone,
                        CAST(ROUND(finalPrice * 100.0) AS INTEGER), 0, 0, CAST(ROUND(finalPrice * 100.0) AS INTEGER),
                        'PAID', 'COMPLETED', 'LOCAL_ONLY', 0, 'legacy_sales_v6', timestamp, timestamp, NULL, timestamp
                    FROM legacy_sales
                """)

                // Migrate Payments
                db.execSQL("""
                    INSERT INTO payments (
                        id, saleId, method, amountMinor, reference, status, receivedAt
                    )
                    SELECT
                        'legacy-payment-v6-' || id, 'legacy-sale-v6-' || id, 'CASH',
                        CAST(ROUND(finalPrice * 100.0) AS INTEGER), NULL, 'SUCCESS', timestamp
                    FROM legacy_sales
                """)

                // Migrate Sale Lines accurately
                db.execSQL("""
                    INSERT INTO sale_lines (
                        id, saleId, productId, productNameSnapshot, barcodeSnapshot, quantityBaseUnits,
                        unitPriceMinor, unitCostMinor, discountMinor, taxMinor, lineTotalMinor, addedAt
                    )
                    SELECT
                        'legacy-sale-line-v6-' || s.id,
                        'legacy-sale-v6-' || s.id,
                        p.id,
                        s.productName,
                        p.ean,
                        s.quantity,
                        CAST(ROUND((s.finalPrice * 100.0) / (CASE WHEN s.quantity <= 0 THEN 1 ELSE s.quantity END)) AS INTEGER),
                        CAST(ROUND(((s.finalPrice - s.profit) * 100.0) / (CASE WHEN s.quantity <= 0 THEN 1 ELSE s.quantity END)) AS INTEGER),
                        0, 0,
                        CAST(ROUND(s.finalPrice * 100.0) AS INTEGER),
                        s.timestamp
                    FROM legacy_sales s
                    LEFT JOIN (
                        SELECT name, MAX(id) as id, ean FROM legacy_products GROUP BY name HAVING COUNT(*) = 1
                    ) p ON s.productName = p.name
                """)

                // Migrate Opening Balances
                db.execSQL("""
                    INSERT INTO stock_movements (
                        id, productId, type, quantityChangeBaseUnits, referenceType, referenceId,
                        note, createdAt, createdBy
                    )
                    SELECT
                        'legacy-stock-movement-' || id, id, 'OPENING_BALANCE', stockQuantity, NULL, NULL,
                        'Migrated from legacy stockQuantity', 0, NULL
                    FROM legacy_products
                    WHERE stockQuantity != 0
                """)

                // Rigorous Validation Checks
                fun assertCount(query: String, expected: Int, errMsg: String) {
                    val cursor = db.query(query)
                    cursor.moveToFirst()
                    val count = cursor.getInt(0)
                    cursor.close()
                    if (count != expected) throw IllegalStateException("$errMsg (Expected $expected, got $count)")
                }

                val cP = db.query("SELECT COUNT(*) FROM legacy_products")
                cP.moveToFirst()
                val legacyProdCount = cP.getInt(0)
                cP.close()

                val cS = db.query("SELECT COUNT(*) FROM legacy_sales")
                cS.moveToFirst()
                val legacySaleCount = cS.getInt(0)
                cS.close()

                val cSt = db.query("SELECT COUNT(*) FROM legacy_products WHERE stockQuantity != 0")
                cSt.moveToFirst()
                val legacyStockCount = cSt.getInt(0)
                cSt.close()

                assertCount("SELECT COUNT(*) FROM products", legacyProdCount, "Product count mismatch")
                assertCount("SELECT COUNT(*) FROM sales", legacySaleCount, "Sale count mismatch")
                assertCount("SELECT COUNT(*) FROM sale_lines", legacySaleCount, "Sale lines count mismatch")
                assertCount("SELECT COUNT(*) FROM sales WHERE id NOT IN (SELECT saleId FROM sale_lines GROUP BY saleId HAVING COUNT(*) = 1)", 0, "Not every sale has exactly one line")
                assertCount("SELECT COUNT(*) FROM stock_movements WHERE type = 'OPENING_BALANCE'", legacyStockCount, "Opening balance count mismatch")
                assertCount("SELECT COUNT(*) FROM stock_movements WHERE type = 'SALE' AND id LIKE 'legacy-%'", 0, "No sale movements should be migrated")
                assertCount("SELECT COUNT(id) - COUNT(DISTINCT id) FROM sales WHERE id LIKE 'legacy-%'", 0, "Duplicate deterministic sales IDs found")
                assertCount("SELECT COUNT(*) FROM sale_lines WHERE productNameSnapshot IS NULL", 0, "Null snapshot found")

                db.execSQL("DROP TABLE legacy_sales")
                db.execSQL("DROP TABLE legacy_products")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_lines_new` (
                        `id` TEXT NOT NULL, `saleId` TEXT NOT NULL,
                        `productId` INTEGER, `productNameSnapshot` TEXT NOT NULL,
                        `barcodeSnapshot` TEXT, `quantityBaseUnits` INTEGER NOT NULL,
                        `unitPriceMinor` INTEGER NOT NULL, `unitCostMinor` INTEGER NOT NULL,
                        `discountMinor` INTEGER NOT NULL, `taxMinor` INTEGER NOT NULL,
                        `lineTotalMinor` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`),
                        FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                    )
                """)
                db.execSQL("INSERT INTO sale_lines_new SELECT * FROM sale_lines")
                db.execSQL("DROP TABLE sale_lines")
                db.execSQL("ALTER TABLE sale_lines_new RENAME TO sale_lines")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_lines_saleId` ON `sale_lines` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_lines_productId` ON `sale_lines` (`productId`)")

                // Keep the backfill logic applying created_at
                db.execSQL("""
                    UPDATE sale_lines
                    SET addedAt = (SELECT createdAt FROM sales WHERE sales.id = sale_lines.saleId)
                    WHERE addedAt = 0
                """)
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to products
                db.execSQL("ALTER TABLE products ADD COLUMN `activeSellingPriceMinor` INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN `pricingUpdatedAt` INTEGER")

                // Backfill: set activeSellingPriceMinor from retailPriceMinor where available, else costPriceMinor
                db.execSQL("""
                    UPDATE products
                    SET activeSellingPriceMinor = CASE
                        WHEN retailPriceMinor IS NOT NULL AND retailPriceMinor > 0 THEN retailPriceMinor
                        WHEN costPriceMinor > 0 THEN costPriceMinor
                        ELSE NULL
                    END,
                    pricingUpdatedAt = updatedAt
                """)

                // 2. Create stock_batches table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `stock_batches` (
                    `id` TEXT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `batchNumber` TEXT,
                    `receivedAt` INTEGER NOT NULL,
                    `expiryAt` INTEGER,
                    `quantityReceived` INTEGER NOT NULL,
                    `quantityRemaining` INTEGER NOT NULL,
                    `unitCostMinor` INTEGER NOT NULL,
                    `intakeSellingPriceMinor` INTEGER NOT NULL,
                    `sourceReference` TEXT,
                    `status` TEXT NOT NULL DEFAULT 'ACTIVE',
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_batches_productId` ON `stock_batches` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_batches_expiryAt` ON `stock_batches` (`expiryAt`)")

                // 3. Create product_price_history table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `product_price_history` (
                    `id` TEXT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `previousPriceMinor` INTEGER,
                    `newPriceMinor` INTEGER NOT NULL,
                    `sourceBatchId` TEXT,
                    `changedAt` INTEGER NOT NULL,
                    `reason` TEXT,
                    `changedBy` TEXT,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_price_history_productId` ON `product_price_history` (`productId`)")

                // 4. Create sale_line_allocations table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `sale_line_allocations` (
                    `id` TEXT NOT NULL,
                    `saleLineId` TEXT NOT NULL,
                    `batchId` TEXT NOT NULL,
                    `quantity` INTEGER NOT NULL,
                    `unitCostMinor` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`saleLineId`) REFERENCES `sale_lines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`batchId`) REFERENCES `stock_batches`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_line_allocations_saleLineId` ON `sale_line_allocations` (`saleLineId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_line_allocations_batchId` ON `sale_line_allocations` (`batchId`)")

                // 5. Create opening batches for existing products that have stock
                val cursor = db.query("SELECT p.id, p.costPriceMinor, COALESCE(p.activeSellingPriceMinor, p.costPriceMinor) as sellPrice, COALESCE(sv.availableBaseUnits, 0) as stock FROM products p LEFT JOIN product_stock_view sv ON p.id = sv.productId WHERE COALESCE(sv.availableBaseUnits, 0) > 0")
                val now = System.currentTimeMillis()
                while (cursor.moveToNext()) {
                    val pid = cursor.getLong(0)
                    val cost = cursor.getLong(1)
                    val sell = cursor.getLong(2)
                    val stock = cursor.getLong(3)
                    val batchId = "migration-batch-$pid"
                    db.execSQL(
                        """INSERT OR IGNORE INTO stock_batches
                           (id, productId, receivedAt, quantityReceived, quantityRemaining, unitCostMinor, intakeSellingPriceMinor, status)
                           VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')""",
                        arrayOf(batchId, pid, now, stock, stock, cost, sell)
                    )
                }
                cursor.close()
            }
        }
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `suppliers` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `phone` TEXT,
                    `address` TEXT,
                    `openingBalanceMinor` INTEGER NOT NULL,
                    `active` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )""")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `purchases` (
                    `id` TEXT NOT NULL,
                    `supplierId` TEXT,
                    `invoiceNumber` TEXT,
                    `purchaseDate` INTEGER NOT NULL,
                    `subtotalMinor` INTEGER NOT NULL,
                    `discountMinor` INTEGER NOT NULL,
                    `taxMinor` INTEGER NOT NULL,
                    `totalMinor` INTEGER NOT NULL,
                    `paidMinor` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`supplierId`) REFERENCES `suppliers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_supplierId` ON `purchases` (`supplierId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_invoiceNumber` ON `purchases` (`invoiceNumber`)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS `purchase_lines` (
                    `id` TEXT NOT NULL,
                    `purchaseId` TEXT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `batchId` TEXT,
                    `quantity` INTEGER NOT NULL,
                    `unitCostMinor` INTEGER NOT NULL,
                    `sellingPriceMinor` INTEGER NOT NULL,
                    `discountMinor` INTEGER NOT NULL,
                    `taxMinor` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`purchaseId`) REFERENCES `purchases`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`productId`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(`batchId`) REFERENCES `stock_batches`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_purchaseId` ON `purchase_lines` (`purchaseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_productId` ON `purchase_lines` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_lines_batchId` ON `purchase_lines` (`batchId`)")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add Product Identity fields
                db.execSQL("ALTER TABLE products ADD COLUMN `brandNameEnglish` TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN `productTypeEnglish` TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN `brandNameTelugu` TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN `productTypeTelugu` TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN `packageAmountBaseUnits` INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN `packageMeasurementType` TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN `teluguEditedByOwner` INTEGER NOT NULL DEFAULT 0")

                // Add Stock Alert Events table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `stock_alert_events` (
                    `id` TEXT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `triggerType` TEXT NOT NULL,
                    `thresholdSnapshotBaseUnits` INTEGER NOT NULL,
                    `stockAtOpenBaseUnits` INTEGER NOT NULL,
                    `openedAt` INTEGER NOT NULL,
                    `resolvedAt` INTEGER,
                    `status` TEXT NOT NULL,
                    `eventSequence` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )""")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_alert_events_productId_status` ON `stock_alert_events` (`productId`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_alert_events_status` ON `stock_alert_events` (`status`)")

                // Add Stock Reminders table
                db.execSQL("""CREATE TABLE IF NOT EXISTS `stock_reminders` (
                    `id` TEXT NOT NULL,
                    `stockEventId` TEXT NOT NULL,
                    `productId` INTEGER NOT NULL,
                    `reminderType` TEXT NOT NULL,
                    `scheduledAt` INTEGER,
                    `lastNotifiedAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    `status` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_reminders_stockEventId` ON `stock_reminders` (`stockEventId`)")
            }
        }
    }
}
