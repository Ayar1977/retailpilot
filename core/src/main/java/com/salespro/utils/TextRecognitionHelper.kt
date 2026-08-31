package com.salespro.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object TextRecognitionHelper {
    fun extractProductDetails(
        context: Context,
        uri: Uri,
        onSuccess: (String, String?) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val lines = visionText.textBlocks.flatMap { it.lines }.map { it.text }

                    // Simple heuristic: The product name is often a prominent text block with letters.
                    val name = lines.firstOrNull { it.length > 3 && it.any { c -> c.isLetter() } } ?: ""

                    // Regex to find MRP/Price looking for 'RS', '₹', 'MRP'
                    val priceMatch = lines.firstNotNullOfOrNull {
                        Regex("""(?:RS|MRP|₹|\bRs\b|\bINR\b)[\s\.]*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
                            .find(it)?.groupValues?.get(1)
                    }

                    onSuccess(name, priceMatch)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun extractInvoiceDetails(
        context: Context,
        uri: Uri,
        onSuccess: (List<Triple<String, Int, Double>>) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // Regex: 1. Item Name, 2. Qty (Int), 3. Price (Double)
                    // Matches lines like: "Lux Soap 20 35.0" or "Maggi Noodles   50   12.0"
                    val regex = Regex("""^(.*?)\s+(\d+)\s+(\d+(?:\.\d+)?)$""")

                    val parsedItems = mutableListOf<Triple<String, Int, Double>>()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val match = regex.find(line.text.trim())
                            if (match != null) {
                                val name = match.groupValues[1].trim()
                                val qty = match.groupValues[2].toIntOrNull() ?: 1
                                val price = match.groupValues[3].toDoubleOrNull() ?: 0.0
                                if (name.isNotEmpty() && price > 0.0) {
                                    parsedItems.add(Triple(name, qty, price))
                                }
                            }
                        }
                    }
                    onSuccess(parsedItems)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    onSuccess(emptyList())
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onSuccess(emptyList())
        }
    }
}
