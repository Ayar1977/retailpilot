package com.salespro.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.salespro.core.R
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

        // Texts follow the language chosen in settings; Arabic mirrors the layout (right-to-left)
        val localized = com.salespro.settings.AppSettings.wrap(context)
        val rtl = localized.resources.configuration.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL
        fun t(id: Int, vararg args: Any): String = localized.getString(id, *args)
        fun drawStart(text: String, x: Float, y: Float, p: Paint) {
            val previous = p.textAlign
            if (rtl) {
                p.textAlign = Paint.Align.RIGHT
                canvas.drawText(text, 595f - x, y, p)
            } else {
                p.textAlign = Paint.Align.LEFT
                canvas.drawText(text, x, y, p)
            }
            p.textAlign = previous
        }

        // Central Invoice Title
        titlePaint.textSize = 18f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(t(R.string.invoice_heading), 595f / 2, 210f, titlePaint)

        // Invoice Metadata
        paint.textSize = 14f
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.US)
        val dateString = dateFormat.format(Date())

        drawStart(t(R.string.invoice_no, "INV-$invoiceNumber"), 40f, 260f, paint)
        drawStart(t(R.string.invoice_date, dateString), 40f, 280f, paint)
        if (customerPhone.isNotBlank()) {
            drawStart(t(R.string.invoice_customer, customerPhone), 40f, 300f, paint)
        }

        // Table Header
        val startY = 340f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawLine(40f, startY - 20f, 555f, startY - 20f, paint)
        drawStart(t(R.string.invoice_col_no), 50f, startY, paint)
        drawStart(t(R.string.invoice_col_desc), 100f, startY, paint)
        drawStart(t(R.string.invoice_col_qty), 350f, startY, paint)
        drawStart(t(R.string.invoice_col_rate), 410f, startY, paint)
        drawStart(t(R.string.invoice_col_amount), 480f, startY, paint)
        canvas.drawLine(40f, startY + 10f, 555f, startY + 10f, paint)

        // Table Content
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val rowY = startY + 40f
        val totalAmount = quantity * pricePerUnit

        drawStart("1", 50f, rowY, paint)
        // Auto-truncate product name to avoid overlapping math columns
        val truncatedName = if (productName.length > 30) productName.take(27) + "..." else productName
        drawStart(truncatedName, 100f, rowY, paint)
        drawStart("$quantity", 350f, rowY, paint)
        drawStart(String.format(Locale.US, "%.2f", pricePerUnit), 410f, rowY, paint)
        drawStart(String.format(Locale.US, "%.2f", totalAmount), 480f, rowY, paint)
        canvas.drawLine(40f, rowY + 20f, 555f, rowY + 20f, paint)

        // Grand Totals Summary
        val totalY = rowY + 60f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        drawStart(t(R.string.invoice_grand_total), 340f, totalY, paint)
        drawStart(com.salespro.settings.Money.formatMajor(totalAmount), 470f, totalY, paint)

        // Standard Store Footer
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val footerY = 842f - 100f
        canvas.drawText(t(R.string.receipt_thanks), 595f / 2, footerY, paint)

        titlePaint.textSize = 14f
        drawStart(t(R.string.invoice_signature), 400f, footerY + 60f, titlePaint)

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
