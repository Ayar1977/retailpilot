package com.salespro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    profile: com.salespro.data.BusinessProfile?,
    isSaving: Boolean,
    onSaveProfile: (businessName: String, ownerName: String, phone: String, address: String, taxNumber: String, receiptFooter: String, logoUri: String?) -> Unit,
    onLoadReferenceProducts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var businessName by remember(profile) { mutableStateOf(profile?.businessName ?: "") }
    var ownerName by remember(profile) { mutableStateOf(profile?.ownerName ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phoneNumber ?: "") }
    var address by remember(profile) { mutableStateOf(profile?.address ?: "") }
    var taxNumber by remember(profile) { mutableStateOf(profile?.taxRegistrationNumber ?: "") }
    var receiptFooter by remember(profile) { mutableStateOf(profile?.receiptFooter ?: "") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Profile") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    onSaveProfile(businessName, ownerName, phone, address, taxNumber, receiptFooter, profile?.logoUri)
                },
                icon = { Icon(Icons.Default.Save, contentDescription = "Save Profile") },
                text = { Text(if (isSaving) "Saving..." else "Save Changes") }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Core Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Receipt & Invoice Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = taxNumber,
                        onValueChange = { taxNumber = it },
                        label = { Text("Tax Registration No. (e.g., GSTIN)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Business Address") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Receipt Footer Message") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Data & Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Load standard reference products to jump-start your initial inventory setup.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onLoadReferenceProducts,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Reference Products")
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
