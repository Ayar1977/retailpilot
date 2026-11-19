package com.salespro.ui

import androidx.compose.ui.res.stringResource
import com.salespro.core.R

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
    onSettingsChanged: () -> Unit = {},
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
                title = { Text(stringResource(R.string.profile_title)) },
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
                icon = { Icon(Icons.Default.Save, contentDescription = stringResource(R.string.save_profile)) },
                text = { Text(if (isSaving) stringResource(R.string.saving) else stringResource(R.string.save_changes)) }
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
            SettingsCard(onSettingsChanged = onSettingsChanged)

            BackupCard()

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.core_information),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text(stringResource(R.string.business_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text(stringResource(R.string.owner_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.phone_number)) },
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
                        stringResource(R.string.receipt_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = taxNumber,
                        onValueChange = { taxNumber = it },
                        label = { Text(stringResource(R.string.tax_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(stringResource(R.string.business_address)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text(stringResource(R.string.receipt_footer)) },
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
                        stringResource(R.string.data_setup),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.data_setup_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = onLoadReferenceProducts,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.add_reference_products))
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
