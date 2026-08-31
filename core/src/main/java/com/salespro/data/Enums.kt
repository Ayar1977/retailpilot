package com.salespro.data

enum class MeasurementType { PIECE, WEIGHT, VOLUME }

enum class SaleStatus { DRAFT, COMPLETED, VOIDED, REFUNDED }

enum class SyncStatus { LOCAL_ONLY, PENDING, SYNCING, SYNCED, FAILED }

enum class PaymentStatus { UNPAID, PARTIALLY_PAID, PAID, REFUNDED }

enum class PaymentMethod { CASH, UPI, CREDIT, OTHER }

enum class StockMovementType {
    OPENING_BALANCE, PURCHASE_RECEIPT, SALE, CUSTOMER_RETURN, SUPPLIER_RETURN,
    DAMAGED, EXPIRED, MANUAL_INCREASE, MANUAL_DECREASE, STOCK_COUNT_CORRECTION
}

enum class StockAlertTriggerType { LOW_STOCK, OUT_OF_STOCK }
enum class StockAlertStatus { ACTIVE, DISMISSED, RESOLVED }
enum class StockReminderType { UPDATE_TODAY, TONIGHT, NOT_NEEDED }
enum class StockReminderStatus { PENDING, NOTIFIED, CANCELLED, COMPLETED }
