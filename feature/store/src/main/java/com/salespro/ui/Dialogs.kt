package com.salespro.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.salespro.data.Product
import java.io.File
import java.io.FileOutputStream

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val directory = File(context.filesDir, "product_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, "product_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)

        // Limit maximum dimension to 1024
        val maxSize = 1024
        val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)

        val finalBitmap = if (scale < 1f) {
            android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }

        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()

        // Cleanup memory safely avoiding recycler conflict if they point to the same object
        if (finalBitmap !== bitmap) {
            finalBitmap.recycle()
        }
        bitmap.recycle()

        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun createImageUri(context: Context): Uri {
    val directory = File(context.filesDir, "camera_cache")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, "temp_camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.applicationContext.packageName}.fileprovider",
        file
    )
}

data class AddProductData(
    val name: String,
    val costPrice: String,
    val retailPrice: String,
    val initialStock: String,
    val imagePath: String,
    val ean: String,
    val brandEnglish: String? = null,
    val typeEnglish: String? = null,
    val brandTelugu: String? = null,
    val typeTelugu: String? = null,
    val packageMeasurement: String? = null,
    val packageUnit: String? = null,
    val displayLanguage: String = "ENGLISH"
)

@Composable
fun AddProductDialog(
    prefilledEan: String = "",
    onDismiss: () -> Unit,
    onConfirm: (AddProductData) -> Unit
) {
    val context = LocalContext.current
    var name by remember(prefilledEan) { mutableStateOf("") }
    var costPrice by remember(prefilledEan) { mutableStateOf("") }
    var retailPrice by remember(prefilledEan) { mutableStateOf("") }
    var initialStock by remember(prefilledEan) { mutableStateOf("") }
    var imagePath by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var ean by remember(prefilledEan) { mutableStateOf(prefilledEan) }

    var showAdvanced by remember(prefilledEan) { mutableStateOf(prefilledEan == "REF-RICE-SETUP") }
    var brandEn by remember(prefilledEan) { mutableStateOf("") }
    var typeEn by remember(prefilledEan) { mutableStateOf("") }
    var brandTe by remember { mutableStateOf("") }
    var typeTe by remember { mutableStateOf("") }
    var pkgMeasure by remember(prefilledEan) { mutableStateOf("") }
    var pkgUnit by remember(prefilledEan) { mutableStateOf("") }
    var displayLanguage by remember { mutableStateOf("ENGLISH") }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoMenu by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val path = saveImageToInternalStorage(context, it)
            if (path != null) {
                imagePath = path
                imageUri = it
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                val path = saveImageToInternalStorage(context, uri)
                if (path != null) {
                    imagePath = path
                    imageUri = uri
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Add New Product",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Photo picker preview & button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { showPhotoMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                var bitmap by remember(imagePath) { mutableStateOf<android.graphics.Bitmap?>(null) }

                                LaunchedEffect(imagePath) {
                                    if (imagePath.isNotEmpty()) {
                                        // Trigger Offline ML OCR Auto-fill
                                        imageUri?.let { uri ->
                                            com.salespro.utils.TextRecognitionHelper.extractProductDetails(context, uri) { extName, extPrice ->
                                                if (name.isBlank() && extName.isNotBlank()) name = extName
                                                if (costPrice.isBlank() && extPrice != null) costPrice = extPrice
                                            }
                                        }

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            try {
                                                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                                                val decoded = context.contentResolver.openInputStream(imageUri!!)?.use {
                                                    BitmapFactory.decodeStream(it, null, options)
                                                }
                                                if (decoded != null) {
                                                    bitmap = decoded
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }

                                bitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Selected Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: run {
                                    Text("+ Photo", style = MaterialTheme.typography.labelSmall)
                                }
                            } else {
                                Text("+ Photo", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        DropdownMenu(
                            expanded = showPhotoMenu,
                            onDismissRequest = { showPhotoMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Take Photo") },
                                onClick = {
                                    showPhotoMenu = false
                                    val uri = createImageUri(context)
                                    tempCameraUri = uri
                                    cameraLauncher.launch(uri)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Choose from Gallery") },
                                onClick = {
                                    showPhotoMenu = false
                                    galleryLauncher.launch("image/*")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = if (imagePath.isNotEmpty()) "Photo attached" else "Tap to add photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Product Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Cost Price / Purchase Price (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = retailPrice,
                    onValueChange = { retailPrice = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Retail Price / Selling Price (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = initialStock,
                    onValueChange = { initialStock = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Initial Stock (Batch Quantity)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ean,
                    onValueChange = { ean = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Barcode (EAN)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showAdvanced, onCheckedChange = { showAdvanced = it })
                    Text("Advanced Identity & Telugu", style = MaterialTheme.typography.bodyMedium)
                }

                if (showAdvanced) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Display Language", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = displayLanguage == "ENGLISH",
                            onClick = { displayLanguage = "ENGLISH" },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = displayLanguage == "TELUGU",
                            onClick = { displayLanguage = "TELUGU" },
                            label = { Text("Telugu") }
                        )
                        FilterChip(
                            selected = displayLanguage == "DUAL",
                            onClick = { displayLanguage = "DUAL" },
                            label = { Text("Dual") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = brandEn, onValueChange = { brandEn = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Brand (English)") },
                        placeholder = { Text(if (prefilledEan == "REF-RICE-SETUP") "e.g., Sree Bell" else "") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = typeEn, onValueChange = { typeEn = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Type (English)") },
                        placeholder = { Text(if (prefilledEan == "REF-RICE-SETUP") "e.g., Sona Masoori" else "") },
                        singleLine = true
                    )

                    if (displayLanguage == "TELUGU" || displayLanguage == "DUAL") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Telugu Localisation (Offline Assisted)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = brandTe, onValueChange = { brandTe = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Brand (Telugu) - బ్రాండ్") }, singleLine = true
                        )
                        OutlinedTextField(
                            value = typeTe, onValueChange = { typeTe = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Type (Telugu) - రకం") }, singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pkgMeasure, onValueChange = { pkgMeasure = it },
                            modifier = Modifier.weight(1f), label = { Text("Measure") },
                            placeholder = { Text(if (prefilledEan == "REF-RICE-SETUP") "e.g., 25" else "") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = pkgUnit, onValueChange = { pkgUnit = it },
                            modifier = Modifier.weight(1f), label = { Text("Unit") },
                            placeholder = { Text(if (prefilledEan == "REF-RICE-SETUP") "e.g., kg" else "") },
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                             val data = AddProductData(
                                name, costPrice, retailPrice, initialStock, imagePath, ean,
                                brandEn.takeIf { it.isNotBlank() },
                                typeEn.takeIf { it.isNotBlank() },
                                brandTe.takeIf { it.isNotBlank() },
                                typeTe.takeIf { it.isNotBlank() },
                                pkgMeasure.takeIf { it.isNotBlank() },
                                pkgUnit.takeIf { it.isNotBlank() },
                                displayLanguage
                            )
                            onConfirm(data)
                        },
                        enabled = name.isNotBlank() && costPrice.isNotBlank() && retailPrice.isNotBlank()
                    ) {
                        Text("Add Product")
                    }
                }
            }
        }
    }
}

@Composable
fun UpdatePriceDialog(
    product: Product,
    newCostPrice: String,
    onPriceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Update Cost Price",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Product: ${product.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Current Cost: ₹${String.format(java.util.Locale.getDefault(), "%.2f", product.costPriceMinor / 100.0)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = newCostPrice,
                    onValueChange = onPriceChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New Cost Price (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = newCostPrice.isNotBlank()
                    ) {
                        Text("Update Price")
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutDialog(
    product: Product,
    inputPrice: String,
    onPriceChange: (String) -> Unit,
    inputQuantity: String,
    onQuantityChange: (String) -> Unit,
    customerPhone: String,
    onPhoneChange: (String) -> Unit,
    sendSmsToggle: Boolean,
    onSendSmsToggleChange: (Boolean) -> Unit,
    generateBillToggle: Boolean,
    onGenerateBillToggleChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val price = inputPrice.toDoubleOrNull() ?: 0.0
    val qty = inputQuantity.toIntOrNull() ?: 1
    val localTotalRevenue = price * qty
    val localProfit = localTotalRevenue - ((product.costPriceMinor / 100.0) * qty)
    val context = androidx.compose.ui.platform.LocalContext.current

    val smsPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            onSendSmsToggleChange(false)
            android.widget.Toast.makeText(context, "SMS Permission Denied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(32.dp, RoundedCornerShape(32.dp), ambientColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Checkout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                InfoRow("Product", product.name)
                InfoRow("Cost", "₹${String.format(java.util.Locale.getDefault(), "%.2f", product.costPriceMinor / 100.0)}")
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputQuantity,
                        onValueChange = onQuantityChange,
                        modifier = Modifier.weight(0.3f),
                        label = { Text("Qty") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inputPrice,
                        onValueChange = onPriceChange,
                        modifier = Modifier.weight(0.7f),
                        label = { Text("Sell Price") },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        InfoRow("Total Sale", "₹${String.format(java.util.Locale.getDefault(), "%.2f", localTotalRevenue)}")
                        InfoRow("Net Profit", "₹${String.format(java.util.Locale.getDefault(), "%.2f", localProfit)}", isProfit = true, isPositive = localProfit >= 0)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Customer Mobile (Optional)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = generateBillToggle, onCheckedChange = onGenerateBillToggleChange)
                    Text("PDF Bill", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(
                        checked = sendSmsToggle,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    smsPermissionLauncher.launch(android.Manifest.permission.SEND_SMS)
                                }
                            }
                            onSendSmsToggleChange(isChecked)
                        }
                    )
                    Text("Auto SMS", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm, enabled = inputPrice.isNotBlank() && inputQuantity.isNotBlank()) {
                        Text("Confirm & Sell")
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    isProfit: Boolean = false,
    isPositive: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (isProfit) MaterialTheme.typography.titleMedium
                   else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isProfit) FontWeight.Bold else FontWeight.Normal,
            color = if (isProfit && !isPositive)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}
