package com.salespro.utils

import android.content.Context
import android.content.Intent

/**
 * Modern receipt sharing mechanism that utilizes the Android Share Sheet.
 * Replaces the legacy SmsHelper which required restricted SEND_SMS permissions.
 */
interface ReceiptShareManager {
    fun shareReceiptText(context: Context, customerPhone: String, productName: String, quantity: Int, totalAmount: Double)
}

class DefaultReceiptShareManager : ReceiptShareManager {
    override fun shareReceiptText(context: Context, customerPhone: String, productName: String, quantity: Int, totalAmount: Double) {
        val message = """
            Receipt from RetailPilot

            Item: $productName x $quantity
            Total Amount: ₹${String.format(java.util.Locale.getDefault(), "%.2f", totalAmount)}

            Thank you for shopping with us!
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }

        val chooserIntent = Intent.createChooser(sendIntent, "Share Receipt")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
