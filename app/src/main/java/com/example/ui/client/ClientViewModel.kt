package com.example.ui.client

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Client
import com.example.data.ClientRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ClientSortOption {
    RECENTLY_ADDED,
    NAME_AZ,
    NAME_ZA
}

class ClientViewModel(
    application: Application,
    private val repository: ClientRepository
) : AndroidViewModel(application) {

    // UI filters
    val searchQuery = MutableStateFlow("")
    val sortBy = MutableStateFlow(ClientSortOption.RECENTLY_ADDED)
    val showInactive = MutableStateFlow(true) // Show inactive toggle

    // Raw clients from repository
    private val allClients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted Client Directory
    val clients: StateFlow<List<Client>> = combine(
        allClients,
        searchQuery,
        sortBy,
        showInactive
    ) { rawClients, query, sort, includeInactive ->
        var list = rawClients

        // 1. Filter by Active status
        if (!includeInactive) {
            list = list.filter { it.isActive }
        }

        // 2. Filter by Search Query
        if (query.isNotBlank()) {
            list = list.filter {
                it.clientName.contains(query, ignoreCase = true) ||
                it.mobileNumber.contains(query) ||
                it.email.contains(query, ignoreCase = true) ||
                it.companyName.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true)
            }
        }

        // 3. Apply Sorting
        when (sort) {
            ClientSortOption.RECENTLY_ADDED -> list.sortedByDescending { it.createdDate }
            ClientSortOption.NAME_AZ -> list.sortedBy { it.clientName.lowercase() }
            ClientSortOption.NAME_ZA -> list.sortedByDescending { it.clientName.lowercase() }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Core CRUD functions
    fun saveClient(client: Client, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = repository.saveClient(
                client.copy(
                    createdDate = if (client.createdDate == 0L) now else client.createdDate,
                    modifiedDate = now
                )
            )
            onComplete(id)
        }
    }

    fun updateClient(client: Client, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateClient(client.copy(modifiedDate = System.currentTimeMillis()))
            onComplete()
        }
    }

    fun deleteClient(client: Client, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteClient(client)
            onComplete()
        }
    }

    fun toggleClientActive(client: Client) {
        viewModelScope.launch {
            val nextActive = !client.isActive
            repository.updateClientStatus(client.clientId, nextActive)
        }
    }

    // Retrieve by mobile (for duplicate check or direct lookup)
    suspend fun getClientByMobile(mobile: String): Client? {
        return repository.getClientByMobile(mobile.trim())
    }

    // Retrieve by ID
    suspend fun getClientById(id: Long): Client? {
        return repository.getClientById(id)
    }
}

class ClientViewModelFactory(
    private val application: Application,
    private val repository: ClientRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClientViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClientViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
