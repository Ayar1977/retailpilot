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
        val localized = com.salespro.settings.AppSettings.wrap(context)
        val message = listOf(
            localized.getString(com.salespro.core.R.string.receipt_title) + " - RetailPilot",
            "",
            localized.getString(com.salespro.core.R.string.receipt_item, productName, quantity),
            localized.getString(com.salespro.core.R.string.receipt_total, com.salespro.settings.Money.formatMajor(totalAmount)),
            "",
            localized.getString(com.salespro.core.R.string.receipt_thanks)
        ).joinToString("\n")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }

        val chooserIntent = Intent.createChooser(sendIntent, com.salespro.settings.AppSettings.wrap(context).getString(com.salespro.core.R.string.share_receipt))
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
