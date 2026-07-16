package com.example.ui.customer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOption {
    RECENTLY_ADDED,
    NAME_AZ,
    NAME_ZA
}

class CustomerViewModel(application: Application, private val repository: QuotesRepository) : AndroidViewModel(application) {
    
    // UI filters
    val searchQuery = MutableStateFlow("")
    val sortBy = MutableStateFlow(SortOption.RECENTLY_ADDED)
    val showInactive = MutableStateFlow(true) // Show inactive toggle

    // Raw customers from repository
    private val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Customer Directory
    val customers: StateFlow<List<CustomerEntity>> = combine(
        allCustomers,
        searchQuery,
        sortBy,
        showInactive
    ) { rawCustomers, query, sort, includeInactive ->
        var list = rawCustomers

        // 1. Filter by Active status
        if (!includeInactive) {
            list = list.filter { it.isActive }
        }

        // 2. Filter by Search Query
        if (query.isNotBlank()) {
            list = list.filter {
                it.customerName.contains(query, ignoreCase = true) ||
                it.mobileNumber.contains(query) ||
                it.siteLocation.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true)
            }
        }

        // 3. Apply Sorting
        when (sort) {
            SortOption.RECENTLY_ADDED -> list.sortedByDescending { it.createdDate }
            SortOption.NAME_AZ -> list.sortedBy { it.customerName.lowercase() }
            SortOption.NAME_ZA -> list.sortedByDescending { it.customerName.lowercase() }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recent Customers list (recently added/modified active customers, limit 5)
    val recentCustomers: StateFlow<List<CustomerEntity>> = allCustomers
        .map { list ->
            list.filter { it.isActive }
                .sortedByDescending { maxOf(it.createdDate, it.modifiedDate) }
                .take(5)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Core CRUD
    fun saveCustomer(customer: CustomerEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveCustomer(customer)
            onComplete(id)
        }
    }

    fun updateCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer.copy(modifiedDate = System.currentTimeMillis()))
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            // Soft delete: update isActive to false, modifiedDate to current
            repository.updateCustomer(customer.copy(isActive = false, modifiedDate = System.currentTimeMillis()))
        }
    }

    fun toggleCustomerActive(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.updateCustomer(customer.copy(isActive = !customer.isActive, modifiedDate = System.currentTimeMillis()))
        }
    }

    // Duplicate Check by mobile number
    suspend fun getCustomerByMobile(mobile: String): CustomerEntity? {
        return repository.getCustomerByMobile(mobile.trim())
    }
}

class CustomerViewModelFactory(private val application: Application, private val repository: QuotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
