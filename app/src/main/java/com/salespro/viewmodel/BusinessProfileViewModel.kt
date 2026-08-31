package com.salespro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salespro.data.BusinessProfile
import com.salespro.data.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class BusinessProfileViewModel @Inject constructor(
    private val repository: SalesRepository
) : ViewModel() {

    val businessProfile = repository.getBusinessProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun saveProfile(
        businessName: String,
        ownerName: String,
        phoneNumber: String,
        address: String,
        taxNumber: String,
        receiptFooter: String,
        logoUri: String?
    ) {
        if (businessName.isBlank()) return

        viewModelScope.launch {
            try {
                _isSaving.value = true
                repository.saveBusinessProfile(
                    BusinessProfile(
                        id = 1,
                        businessName = businessName,
                        ownerName = ownerName.takeIf { it.isNotBlank() },
                        phoneNumber = phoneNumber.takeIf { it.isNotBlank() },
                        address = address.takeIf { it.isNotBlank() },
                        taxRegistrationNumber = taxNumber.takeIf { it.isNotBlank() },
                        receiptFooter = receiptFooter.takeIf { it.isNotBlank() },
                        logoUri = logoUri
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // handle error
            } finally {
                _isSaving.value = false
            }
        }
    }
    fun loadReferenceProducts() {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                repository.setupReferenceProducts()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isSaving.value = false
            }
        }
    }
}
