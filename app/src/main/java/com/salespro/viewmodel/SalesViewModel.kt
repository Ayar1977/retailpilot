package com.salespro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salespro.data.CartResult
import com.salespro.data.Product
import com.salespro.data.Sale
import com.salespro.data.SaleResult
import com.salespro.data.SalesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val repository: SalesRepository,
    private val receiptShareManager: com.salespro.utils.ReceiptShareManager
) : ViewModel() {

    val products = repository.getAllProducts()
    val sales = repository.getAllSales()
    val purchases = repository.getAllPurchases()

    val activeStockAlerts = repository.getActiveStockAlerts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingReminders = repository.getPendingStockReminders().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeCartLines = repository.observeActiveDraft()
        .flatMapLatest { draft ->
            if (draft != null) repository.observeCartLines(draft.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI event channel ---
    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvent = _uiEvent.asSharedFlow()

    // --- Double-tap protection ---
    private val _completionInProgress = MutableStateFlow(false)
    val completionInProgress: StateFlow<Boolean> = _completionInProgress.asStateFlow()

    fun updateCartQuantity(line: com.salespro.data.SaleLine, quantity: Long) {
        viewModelScope.launch {
            try {
                if (quantity <= 0) {
                    repository.deleteLine(line.id)
                } else {
                    repository.updateLineQuantity(line.id, quantity)
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Failed to update quantity"))
            }
        }
    }

    /**
     * Add to cart using authoritative pricing. No cashier price entry.
     */
    fun addToCart(productId: Long, quantity: Long) {
        viewModelScope.launch {
            try {
                when (val result = repository.addToCart(productId, quantity)) {
                    is CartResult.Success -> { /* cart updated reactively */ }
                    is CartResult.MissingSellingPrice -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Set a selling price for ${result.productName} in Inventory first."))
                    }
                    is CartResult.InsufficientStock -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Insufficient stock for ${result.productName} (${result.available} available)"))
                    }
                    is CartResult.ProductNotFound -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Product not found"))
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Failed to add to cart"))
            }
        }
    }

    /**
     * Complete checkout with double-tap protection and SaleResult handling.
     */
    fun completeCartCheckout(context: android.content.Context) {
        if (_completionInProgress.value) return // double-tap guard

        viewModelScope.launch {
            _completionInProgress.value = true
            try {
                val draft = repository.observeActiveDraft().firstOrNull()
                if (draft == null) {
                    _uiEvent.tryEmit(UiEvent.ShowError("No active cart to complete"))
                    return@launch
                }

                val lines = repository.observeCartLines(draft.id).firstOrNull() ?: emptyList()
                val totalMinor = lines.sumOf { it.lineTotalMinor }

                when (val result = repository.completeSale(draft.id, totalMinor)) {
                    is SaleResult.Success -> {
                        _uiEvent.tryEmit(UiEvent.ShowSuccess("Sale completed! Total: ₹${result.totalMinor / 100}.${String.format("%02d", result.totalMinor % 100)}"))
                    }
                    is SaleResult.InsufficientStock -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Insufficient stock for ${result.productName} (${result.available} available, ${result.requested} needed). Review your cart."))
                    }
                    is SaleResult.MissingSellingPrice -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Set a selling price for ${result.productName} in Inventory."))
                    }
                    is SaleResult.InvalidQuantity -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Invalid quantity for ${result.productName}."))
                    }
                    is SaleResult.EmptyCart -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Cart is empty."))
                    }
                    is SaleResult.InvalidPayment -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Invalid payment amount."))
                    }
                    is SaleResult.AlreadyCompleted -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("This sale is already completed."))
                    }
                    is SaleResult.DatabaseFailure -> {
                        _uiEvent.tryEmit(UiEvent.ShowError("Database error: ${result.message}"))
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Unexpected error"))
            } finally {
                _completionInProgress.value = false
            }
        }
    }

    /**
     * Receive inventory batch.
     */
    fun receiveInventoryBatch(
        productId: Long,
        quantity: Long,
        unitCostMinor: Long,
        sellingPriceMinor: Long,
        batchNumber: String? = null,
        expiryAt: Long? = null,
        applySellingPriceToProduct: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                repository.receiveInventoryBatch(
                    productId = productId,
                    quantity = quantity,
                    unitCostMinor = unitCostMinor,
                    sellingPriceMinor = sellingPriceMinor,
                    batchNumber = batchNumber,
                    expiryAt = expiryAt,
                    applySellingPriceToProduct = applySellingPriceToProduct
                )
                _uiEvent.tryEmit(UiEvent.ShowSuccess("Stock received: $quantity units"))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Failed to receive stock"))
            }
        }
    }

    /**
     * Receive entire purchase transactionally.
     */
    fun receivePurchase(purchase: com.salespro.data.Purchase, lines: List<com.salespro.data.PurchaseLineData>) {
        viewModelScope.launch {
            try {
                repository.receivePurchase(purchase, lines)
                _uiEvent.tryEmit(UiEvent.ShowSuccess("Purchase received successfully!"))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Failed to receive purchase"))
            }
        }
    }

    // --- Legacy search/dialog state (retained for compatibility) ---
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _showAddProductDialog = MutableStateFlow(false)
    val showAddProductDialog: StateFlow<Boolean> = _showAddProductDialog.asStateFlow()

    private val _showUpdatePriceDialog = MutableStateFlow(false)
    val showUpdatePriceDialog: StateFlow<Boolean> = _showUpdatePriceDialog.asStateFlow()

    private val _productToUpdate = MutableStateFlow<Product?>(null)
    val productToUpdate: StateFlow<Product?> = _productToUpdate.asStateFlow()

    private val _newCostPrice = MutableStateFlow("")
    val newCostPrice: StateFlow<String> = _newCostPrice.asStateFlow()

    private val _showPriceInputDialog = MutableStateFlow(false)
    val showPriceInputDialog: StateFlow<Boolean> = _showPriceInputDialog.asStateFlow()

    private val _inputPrice = MutableStateFlow("")
    val inputPrice: StateFlow<String> = _inputPrice.asStateFlow()

    private val _inputQuantity = MutableStateFlow("1")
    val inputQuantity: StateFlow<String> = _inputQuantity.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _sendSmsToggle = MutableStateFlow(false)
    val sendSmsToggle: StateFlow<Boolean> = _sendSmsToggle.asStateFlow()

    private val _generateBillToggle = MutableStateFlow(true)
    val generateBillToggle: StateFlow<Boolean> = _generateBillToggle.asStateFlow()

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun onProductSelected(product: Product) {
        _selectedProduct.value = product
        _showPriceInputDialog.value = true
    }

    fun onAddProductDialogShow() {
        _showAddProductDialog.value = true
    }

    private val _prefilledEan = MutableStateFlow("")
    val prefilledEan: StateFlow<String> = _prefilledEan.asStateFlow()

    private val _hapticTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hapticTrigger = _hapticTrigger.asSharedFlow()

    // --- Inventory intake dialog state ---
    private val _showReceiveStockDialog = MutableStateFlow(false)
    val showReceiveStockDialog: StateFlow<Boolean> = _showReceiveStockDialog.asStateFlow()

    private val _receiveStockProduct = MutableStateFlow<Product?>(null)
    val receiveStockProduct: StateFlow<Product?> = _receiveStockProduct.asStateFlow()

    fun onReceiveStockShow(product: Product) {
        _receiveStockProduct.value = product
        _showReceiveStockDialog.value = true
    }

    fun onReceiveStockDismiss() {
        _showReceiveStockDialog.value = false
        _receiveStockProduct.value = null
    }

    fun onAddProductDialogDismiss() {
        _showAddProductDialog.value = false
        _prefilledEan.value = ""
        _riceSetupRequested.value = false
    }

    private val _riceSetupRequested = MutableStateFlow(false)
    val riceSetupRequested: StateFlow<Boolean> = _riceSetupRequested.asStateFlow()

    fun onSetupRiceRequested() {
        _prefilledEan.value = "REF-RICE-SETUP"
        _riceSetupRequested.value = true
        _showAddProductDialog.value = true
    }

    fun onUpdatePriceDialogShow(product: Product) {
        _productToUpdate.value = product
        _newCostPrice.value = (product.costPriceMinor / 100.0).toString()
        _showUpdatePriceDialog.value = true
    }

    fun onUpdatePriceDialogDismiss() {
        _showUpdatePriceDialog.value = false
        _productToUpdate.value = null
        _newCostPrice.value = ""
    }

    fun onNewCostPriceChange(price: String) {
        _newCostPrice.value = price
    }

    fun updateProductPrice() {
        val product = _productToUpdate.value ?: return
        val price = _newCostPrice.value.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.updateProductPrice(product.id, (price * 100).toLong())
        }

        _showUpdatePriceDialog.value = false
        _productToUpdate.value = null
        _newCostPrice.value = ""
    }

    fun onPriceInputDialogDismiss() {
        _showPriceInputDialog.value = false
        _inputPrice.value = ""
        _inputQuantity.value = "1"
        _customerPhone.value = ""
        _selectedProduct.value = null
    }

    fun onInputPriceChange(price: String) { _inputPrice.value = price }
    fun onInputQuantityChange(qty: String) { _inputQuantity.value = qty }
    fun onCustomerPhoneChange(phone: String) { _customerPhone.value = phone }
    fun onSendSmsToggleChange(enabled: Boolean) { _sendSmsToggle.value = enabled }
    fun onGenerateBillToggleChange(enabled: Boolean) { _generateBillToggle.value = enabled }

    /**
     * Legacy confirmSale for PriceInputDialog retained for compatibility.
     * Uses authoritative pricing - the inputPrice is ignored for the addToCart path.
     */
    fun confirmSale(context: android.content.Context) {
        val product = _selectedProduct.value ?: return
        val qty = _inputQuantity.value.toLongOrNull() ?: 1L
        val phone = _customerPhone.value

        viewModelScope.launch {
            try {
                // Use addToCart with authoritative pricing
                val cartResult = repository.addToCart(product.id, qty)
                when (cartResult) {
                    is CartResult.Success -> {
                        // Auto-complete the sale
                        val draft = repository.observeActiveDraft().firstOrNull()
                        if (draft != null) {
                            val lines = repository.observeCartLines(draft.id).firstOrNull() ?: emptyList()
                            val totalMinor = lines.sumOf { it.lineTotalMinor }
                            val result = repository.completeSale(draft.id, totalMinor)
                            when (result) {
                                is SaleResult.Success -> {
                                    _uiEvent.tryEmit(UiEvent.ShowSuccess("Sale completed!"))
                                    if (_generateBillToggle.value) {
                                        val price = (result.totalMinor / 100.0)
                                        val file = com.salespro.utils.InvoiceGenerator.generateInvoiceOutput(context, product.name, qty.toInt(), price, phone)
                                        file?.let {
                                            try {
                                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.applicationContext.packageName}.fileprovider", it)
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                                                intent.setDataAndType(uri, "application/pdf")
                                                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                context.startActivity(intent)
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }
                                    }
                                    if (_sendSmsToggle.value) {
                                        receiptShareManager.shareReceiptText(context, phone, product.name, qty.toInt(), result.totalMinor / 100.0)
                                    }
                                }
                                is SaleResult.InsufficientStock -> _uiEvent.tryEmit(UiEvent.ShowError("Insufficient stock for ${result.productName}"))
                                is SaleResult.MissingSellingPrice -> _uiEvent.tryEmit(UiEvent.ShowError("Set selling price for ${result.productName} first"))
                                else -> _uiEvent.tryEmit(UiEvent.ShowError("Failed to complete sale"))
                            }
                        }
                    }
                    is CartResult.MissingSellingPrice -> _uiEvent.tryEmit(UiEvent.ShowError("Set a selling price for ${cartResult.productName} in Inventory first."))
                    is CartResult.InsufficientStock -> _uiEvent.tryEmit(UiEvent.ShowError("Insufficient stock for ${cartResult.productName}"))
                    is CartResult.ProductNotFound -> _uiEvent.tryEmit(UiEvent.ShowError("Product not found"))
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Failed to complete sale"))
            }
        }

        _showPriceInputDialog.value = false
        _inputPrice.value = ""
        _inputQuantity.value = "1"
        _customerPhone.value = ""
        _selectedProduct.value = null
    }

    fun deleteSale(sale: Sale) {
        viewModelScope.launch {
            try {
                repository.deleteSaleSafely(sale)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Cannot delete this sale"))
            }
        }
    }

    fun addProduct(data: com.salespro.ui.AddProductData) {
        val costMinor = ((data.costPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val retailMinor = ((data.retailPrice.toDoubleOrNull() ?: 0.0) * 100).toLong()
        val stockQty = data.initialStock.toLongOrNull() ?: 0L

        val pkgUnitType = when (data.packageUnit?.lowercase()?.trim()) {
            "kg", "g", "mg" -> com.salespro.data.MeasurementType.WEIGHT
            "l", "ml" -> com.salespro.data.MeasurementType.VOLUME
            "pc", "pcs", "pkt", "box" -> com.salespro.data.MeasurementType.PIECE
            else -> com.salespro.data.MeasurementType.PIECE
        }

        val finalBarcode = if (data.ean == "REF-RICE-SETUP") "" else data.ean

        val product = Product(
            name = data.name,
            costPriceMinor = costMinor,
            retailPriceMinor = retailMinor,
            activeSellingPriceMinor = if (retailMinor > 0) retailMinor else null,
            pricingUpdatedAt = System.currentTimeMillis(),
            priceNeedsReview = retailMinor <= 0,
            imageUri = data.imagePath,
            barcode = finalBarcode,
            brandNameEnglish = data.brandEnglish,
            productTypeEnglish = data.typeEnglish,
            brandNameTelugu = data.brandTelugu,
            productTypeTelugu = data.typeTelugu,
            packageAmountBaseUnits = data.packageMeasurement?.toLongOrNull(),
            packageMeasurementType = pkgUnitType,
            teluguEditedByOwner = data.displayLanguage == "TELUGU" || data.displayLanguage == "DUAL"
        )

        viewModelScope.launch {
            try {
                val productId = repository.insertProduct(product)

                if (data.ean == "REF-RICE-SETUP") {
                    repository.insertReferenceLink("RICE_REFERENCE", productId)
                }

                if (stockQty > 0 && retailMinor > 0) {
                    repository.receiveInventoryBatch(
                        productId = productId,
                        quantity = stockQty,
                        unitCostMinor = costMinor,
                        sellingPriceMinor = retailMinor,
                        applySellingPriceToProduct = true
                    )
                    _uiEvent.tryEmit(UiEvent.ShowSuccess("${data.name} added with $stockQty units"))
                } else if (stockQty > 0) {
                    // Stock but no selling price - just add opening balance movement
                    repository.insertStockMovement(com.salespro.data.StockMovement(
                        productId = productId,
                        type = com.salespro.data.StockMovementType.OPENING_BALANCE,
                        quantityChangeBaseUnits = stockQty,
                        referenceType = "INITIAL_STOCK",
                        note = "Initial stock added during product creation"
                    ))
                    _uiEvent.tryEmit(UiEvent.ShowSuccess("${data.name} added (set selling price to enable sales)"))
                } else {
                    _uiEvent.tryEmit(UiEvent.ShowSuccess("${data.name} added (add stock to enable sales)"))
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError(e.message ?: "Failed to add product"))
            }
        }

        _showAddProductDialog.value = false
        _prefilledEan.value = ""
    }

    fun onBarcodeScanned(eanCode: String) {
        viewModelScope.launch {
            val product = repository.getProductByBarcode(eanCode)
            if (product != null) {
                // Add to cart directly with authoritative price
                addToCart(product.id, 1L)
            } else {
                _prefilledEan.value = eanCode
                _showAddProductDialog.value = true
            }
        }
    }

    fun startScan(context: android.content.Context) {
        try {
            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    _hapticTrigger.tryEmit(Unit)
                    barcode.rawValue?.let {
                        onBarcodeScanned(it)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importProductsFromInvoice(items: List<Triple<String, Int, Double>>) {
        viewModelScope.launch {
            for (item in items) {
                val (name, qty, price) = item
                val existing = repository.getProductByName(name)
                if (existing != null) {
                    val updated = existing.copy(
                        costPriceMinor = (price * 100).toLong()
                    )
                    repository.insertProduct(updated)
                } else {
                    val newProduct = Product(
                        name = name,
                        costPriceMinor = (price * 100).toLong(),
                        imageUri = name.lowercase().replace(" ", "_"),
                        barcode = ""
                    )
                    repository.insertProduct(newProduct)
                }
            }
        }
    }

    fun onInvoiceImageSelected(context: android.content.Context, uri: android.net.Uri) {
        com.salespro.utils.TextRecognitionHelper.extractInvoiceDetails(context, uri) { items ->
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            handler.post {
                if (items.isNotEmpty()) {
                    importProductsFromInvoice(items)
                    android.widget.Toast.makeText(context, "Parsed ${items.size} items from Invoice!", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to parse any items...", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun markReminderCompleted(id: String) {
        viewModelScope.launch {
            try {
                repository.markReminderCompleted(id)
            } catch (e: Exception) {
                _uiEvent.tryEmit(UiEvent.ShowError("Could not clear reminder"))
            }
        }
    }

    /**
     * UI event types for one-time display.
     */
    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowSuccess(val message: String) : UiEvent()
    }
}
