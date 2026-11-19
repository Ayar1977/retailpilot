package com.salespro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.salespro.settings.AppLanguage
import com.salespro.settings.AppSettings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import com.salespro.ui.AddProductDialog
import com.salespro.ui.AnalyticsScreen
import com.salespro.ui.CheckoutDialog
import com.salespro.ui.InventoryScreen
import com.salespro.ui.SalesScreen
import com.salespro.ui.UpdatePriceDialog
import com.salespro.viewmodel.SalesViewModel
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SalesViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        // Apply the language chosen in More > Settings (Arabic switches the layout to right-to-left).
        super.attachBaseContext(AppSettings.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.salespro.ui.theme.SalesProTheme {
                val layoutDirection = when (AppSettings.language) {
                    AppLanguage.ARABIC -> LayoutDirection.Rtl
                    AppLanguage.ENGLISH -> LayoutDirection.Ltr
                    AppLanguage.SYSTEM -> LocalLayoutDirection.current
                }
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    RetailPilotApp(
                        viewModel = viewModel,
                        onSettingsChanged = { recreate() }
                    )
                }
            }
        }
    }
}

@Composable
fun RetailPilotApp(
    viewModel: SalesViewModel,
    onSettingsChanged: () -> Unit = {}
) {
    val stateHolder = rememberSaveableStateHolder()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val prefilledEan by viewModel.prefilledEan.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Consume UiEvent one-time messages
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is SalesViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is SalesViewModel.UiEvent.ShowSuccess -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            com.salespro.ui.AdaptiveNavigation { destination ->
                stateHolder.SaveableStateProvider(destination) {
                    when (destination) {
                        "Sell" -> {
                            val products by viewModel.products.collectAsState(initial = emptyList())
                            val searchText by viewModel.searchText.collectAsState()
                            val selectedProduct by viewModel.selectedProduct.collectAsState()
                            val showUpdatePriceDialog by viewModel.showUpdatePriceDialog.collectAsState()
                            val productToUpdate by viewModel.productToUpdate.collectAsState()
                            val newCostPrice by viewModel.newCostPrice.collectAsState()

                            val showPriceInputDialog by viewModel.showPriceInputDialog.collectAsState()
                            val inputPrice by viewModel.inputPrice.collectAsState()
                            val inputQuantity by viewModel.inputQuantity.collectAsState()
                            val customerPhone by viewModel.customerPhone.collectAsState()
                            val sendSmsToggle by viewModel.sendSmsToggle.collectAsState()
                            val generateBillToggle by viewModel.generateBillToggle.collectAsState()
                            val activeCartLines by viewModel.activeCartLines.collectAsState()
                            val completionInProgress by viewModel.completionInProgress.collectAsState()

                            SalesScreen(
                                products = products.filter { it.name.contains(searchText, ignoreCase = true) },
                                cartLines = activeCartLines,
                                onProductSelected = { p -> viewModel.addToCart(p.id, 1L) },
                                onUpdateQuantity = viewModel::updateCartQuantity,
                                onCheckout = { viewModel.completeCartCheckout(context) },
                                isCheckoutEnabled = !completionInProgress,
                                onScan = { viewModel.startScan(context) }
                            )

                            if (showUpdatePriceDialog && productToUpdate != null) {
                                UpdatePriceDialog(
                                    product = productToUpdate!!,
                                    newCostPrice = newCostPrice,
                                    onPriceChange = viewModel::onNewCostPriceChange,
                                    onDismiss = viewModel::onUpdatePriceDialogDismiss,
                                    onConfirm = viewModel::updateProductPrice
                                )
                            }

                            if (showPriceInputDialog && selectedProduct != null) {
                                CheckoutDialog(
                                    product = selectedProduct!!,
                                    inputPrice = inputPrice,
                                    inputQuantity = inputQuantity,
                                    customerPhone = customerPhone,
                                    sendSmsToggle = sendSmsToggle,
                                    generateBillToggle = generateBillToggle,
                                    onPriceChange = viewModel::onInputPriceChange,
                                    onQuantityChange = viewModel::onInputQuantityChange,
                                    onPhoneChange = viewModel::onCustomerPhoneChange,
                                    onSendSmsToggleChange = viewModel::onSendSmsToggleChange,
                                    onGenerateBillToggleChange = viewModel::onGenerateBillToggleChange,
                                    onDismiss = viewModel::onPriceInputDialogDismiss,
                                    onConfirm = { viewModel.confirmSale(context) }
                                )
                            }
                        }
                        "Inventory" -> {
                            val products by viewModel.products.collectAsState(initial = emptyList())
                            val activeAlerts by viewModel.activeStockAlerts.collectAsState()
                            val pendingReminders by viewModel.pendingReminders.collectAsState()
                            val packagings by viewModel.packagings.collectAsState(initial = emptyList())
                            InventoryScreen(
                                products = products,
                                activeAlerts = activeAlerts,
                                pendingReminders = pendingReminders,
                                onDismissReminder = viewModel::markReminderCompleted,
                                onAddProduct = viewModel::onAddProductDialogShow,
                                onScanBarcode = { viewModel.startScanForNewProduct(context) },
                                packagings = packagings,
                                onAddPackaging = viewModel::addPackaging,
                                onDeletePackaging = viewModel::deletePackaging,
                                onScanInto = { onCode -> viewModel.scanBarcode(context, onCode) },
                                onUpdateProduct = viewModel::updateProductDetails,
                                onReceiveStock = { productId, qty, costMinor, sellingMinor, batchNumber ->
                                    viewModel.receiveInventoryBatch(
                                        productId = productId,
                                        quantity = qty,
                                        unitCostMinor = costMinor,
                                        sellingPriceMinor = sellingMinor,
                                        batchNumber = batchNumber
                                    )
                                }
                            )
                        }
                        "Purchases" -> {
                            val purchases by viewModel.purchases.collectAsState(initial = emptyList())
                            val purchaseLines by viewModel.purchaseLines.collectAsState(initial = emptyList())
                            val products by viewModel.products.collectAsState(initial = emptyList())
                            com.salespro.ui.PurchasesScreen(
                                purchases = purchases,
                                purchaseLines = purchaseLines,
                                products = products,
                                onReceivePurchase = viewModel::receivePurchase,
                                onScanForPurchase = { onResult -> viewModel.scanForPurchase(context, onResult) }
                            )
                        }
                        "Reports" -> {
                            val sales by viewModel.sales.collectAsState(initial = emptyList())
                            AnalyticsScreen(sales = sales)
                        }
                        "More" -> {
                            val bpViewModel: com.salespro.viewmodel.BusinessProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                            val profile by bpViewModel.businessProfile.collectAsState()
                            val isSaving by bpViewModel.isSaving.collectAsState()

                            com.salespro.ui.BusinessProfileScreen(
                                profile = profile,
                                isSaving = isSaving,
                                onSaveProfile = bpViewModel::saveProfile,
                                onLoadReferenceProducts = {
                                    bpViewModel.loadReferenceProducts() // Inserts Coke
                                    viewModel.onSetupRiceRequested() // Triggers Rice form
                                },
                                onSettingsChanged = onSettingsChanged
                            )
                        }
                    }
                }
            }

            // Available from every tab (Inventory button, barcode scan, More > reference products)
            if (showAddProductDialog) {
                AddProductDialog(
                    prefilledEan = prefilledEan,
                    onDismiss = viewModel::onAddProductDialogDismiss,
                    onConfirm = viewModel::addProduct
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
