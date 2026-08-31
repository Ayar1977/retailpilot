package com.salespro.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.salespro.data.AppDatabase
import com.salespro.data.StockReminderStatus
import com.salespro.data.StockReminderType
import kotlinx.coroutines.flow.firstOrNull

class StockReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.salesDao()

        val notificationHelper = NotificationHelper(applicationContext)

        // Fetch pending reminders
        val pendingReminders = dao.getPendingStockReminders().firstOrNull() ?: emptyList()
        val now = System.currentTimeMillis()

        for (reminder in pendingReminders) {
            // Check if it's time to notify (if scheduledAt exists it must be <= now)
            val schedAt = reminder.scheduledAt
            if (schedAt != null && schedAt > now) {
                continue
            }

            // Generate notification content
            val product = dao.getProductById(reminder.productId) ?: continue
            val stock = dao.getStockForProduct(reminder.productId)
            val available = stock?.availableBaseUnits ?: 0

            // For now, basic notification
            notificationHelper.showStockAlertNotification(
                id = reminder.id.hashCode(),
                title = "Low Stock Alert: ${product.name}",
                content = "You have $available left in stock."
            )

            // Mark as notified / completed (for brevity we complete it)
            dao.updateStockReminderStatus(reminder.id, StockReminderStatus.COMPLETED, now)
        }

        return Result.success()
    }
}
