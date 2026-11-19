package com.salespro.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoiceGenerator {

    fun generateInvoiceOutput(
        context: Context,
        productName: String,
        quantity: Int,
        pricePerUnit: Double,
        customerPhone: String,
        invoiceNumber: String = System.currentTimeMillis().toString().takeLast(6)
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // standard A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Store Header Section
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.textSize = 24f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.color = Color.rgb(128, 0, 128) // Purple
        canvas.drawText("RetailPilot", 595f / 2, 80f, titlePaint)

        titlePaint.textSize = 14f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        titlePaint.color = Color.BLACK
        canvas.drawText("", 595f / 2, 105f, titlePaint)
        canvas.drawText("", 595f / 2, 125f, titlePaint)
        canvas.drawText("", 595f / 2, 145f, titlePaint)
        canvas.drawText("", 595f / 2, 165f, titlePaint)

        // Central Invoice Title
        titlePaint.textSize = 18f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TAX INVOICE / BILL", 595f / 2, 210f, titlePaint)

        // Invoice Metadata
        paint.textSize = 14f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        canvas.drawText("Invoice No: INV-$invoiceNumber", 40f, 260f, paint)
        canvas.drawText("Date: $dateString", 40f, 280f, paint)
        if (customerPhone.isNotBlank()) {
            canvas.drawText("Customer Mobile: $customerPhone", 40f, 300f, paint)
        }

        // Table Header
        val startY = 340f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(40f, startY - 20f, 555f, startY - 20f, paint)
        canvas.drawText("S.No", 50f, startY, paint)
        canvas.drawText("Description", 100f, startY, paint)
        canvas.drawText("Qty", 350f, startY, paint)
        canvas.drawText("Rate", 410f, startY, paint)
        canvas.drawText("Amount", 480f, startY, paint)
        canvas.drawLine(40f, startY + 10f, 555f, startY + 10f, paint)

        // Table Content
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val rowY = startY + 40f
        val totalAmount = quantity * pricePerUnit

        canvas.drawText("1", 50f, rowY, paint)
        // Auto-truncate product name to avoid overlapping math columns
        val truncatedName = if (productName.length > 30) productName.take(27) + "..." else productName
        canvas.drawText(truncatedName, 100f, rowY, paint)
        canvas.drawText("$quantity", 350f, rowY, paint)
        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.2f", pricePerUnit), 410f, rowY, paint)
        canvas.drawText(String.format(java.util.Locale.getDefault(), "%.2f", totalAmount), 480f, rowY, paint)
        canvas.drawLine(40f, rowY + 20f, 555f, rowY + 20f, paint)

        // Grand Totals Summary
        val totalY = rowY + 60f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Grand Total:", 380f, totalY, paint)
        canvas.drawText(com.salespro.settings.Money.formatMajor(totalAmount), 480f, totalY, paint)

        // Standard Store Footer
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val footerY = 842f - 100f
        canvas.drawText("Thank you for shopping with us!", 595f / 2, footerY, paint)
        canvas.drawText("High-quality rice of all brands available with door delivery.", 595f / 2, footerY + 20f, paint)

        titlePaint.textAlign = Paint.Align.RIGHT
        titlePaint.textSize = 14f
        canvas.drawText("Authorized Signatory", 555f, footerY + 60f, titlePaint)

        pdfDocument.finishPage(page)

        try {
            val directory = File(context.cacheDir, "shared/receipts")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "Invoice_$invoiceNumber.pdf"
            val file = File(directory, fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }
}
