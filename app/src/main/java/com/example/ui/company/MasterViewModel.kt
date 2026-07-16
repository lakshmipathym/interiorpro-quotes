package com.example.ui.company

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MasterEntity
import com.example.data.MasterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MasterSortOption {
    NAME_AZ,
    NAME_ZA,
    DISPLAY_ORDER
}

class MasterViewModel(application: Application, private val repository: MasterRepository) : AndroidViewModel(application) {

    // UI State / Filters
    val searchQuery = MutableStateFlow("")
    val sortBy = MutableStateFlow(MasterSortOption.DISPLAY_ORDER)
    val showInactive = MutableStateFlow(true)

    // Master list from repository
    private val allMasters: StateFlow<List<MasterEntity>> = repository.getAllMasters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered & Sorted master records
    fun getFilteredMasters(masterType: String): Flow<List<MasterEntity>> {
        return combine(
            allMasters,
            searchQuery,
            sortBy,
            showInactive
        ) { rawMasters, query, sort, includeInactive ->
            var list = rawMasters.filter { it.masterType == masterType && !it.isDeleted }

            // 1. Filter active
            if (!includeInactive) {
                list = list.filter { it.isActive }
            }

            // 2. Search filter
            if (query.isNotBlank()) {
                list = list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.description.contains(query, ignoreCase = true)
                }
            }

            // 3. Sorting
            when (sort) {
                MasterSortOption.DISPLAY_ORDER -> list.sortedWith(compareBy<MasterEntity> { it.displayOrder }.thenBy { it.name.lowercase() })
                MasterSortOption.NAME_AZ -> list.sortedBy { it.name.lowercase() }
                MasterSortOption.NAME_ZA -> list.sortedByDescending { it.name.lowercase() }
            }
        }.flowOn(Dispatchers.Default)
    }

    // CRUD Methods
    fun saveMaster(
        masterType: String,
        name: String,
        description: String,
        displayOrder: Int,
        onDuplicate: () -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        viewModelScope.launch {
            // Check duplicates
            val existing = repository.getMasterByTypeAndName(masterType, trimmedName)
            if (existing != null) {
                onDuplicate()
            } else {
                val newMaster = MasterEntity(
                    masterType = masterType,
                    name = trimmedName,
                    description = description.trim(),
                    displayOrder = displayOrder,
                    isActive = true,
                    isDeleted = false,
                    createdDate = System.currentTimeMillis(),
                    modifiedDate = System.currentTimeMillis()
                )
                repository.saveMaster(newMaster)
                onSuccess()
            }
        }
    }

    fun updateMaster(
        master: MasterEntity,
        name: String,
        description: String,
        displayOrder: Int,
        isActive: Boolean,
        onDuplicate: () -> Unit = {},
        onSuccess: () -> Unit = {}
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return

        viewModelScope.launch {
            // Check duplicates if name changed
            if (!trimmedName.equals(master.name, ignoreCase = true)) {
                val existing = repository.getMasterByTypeAndName(master.masterType, trimmedName)
                if (existing != null) {
                    onDuplicate()
                    return@launch
                }
            }

            val updated = master.copy(
                name = trimmedName,
                description = description.trim(),
                displayOrder = displayOrder,
                isActive = isActive,
                modifiedDate = System.currentTimeMillis()
            )
            repository.updateMaster(updated)
            onSuccess()
        }
    }

    fun toggleMasterActive(master: MasterEntity) {
        viewModelScope.launch {
            repository.updateMaster(master.copy(isActive = !master.isActive, modifiedDate = System.currentTimeMillis()))
        }
    }

    /**
     * Delete Master rule:
     * - If master is already used in quotations, deactivate it instead of soft-deleting.
     * - Otherwise, soft-delete it.
     * Returns true if soft-deleted, false if deactivated due to being in use.
     */
    fun deleteMaster(master: MasterEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val inUse = repository.isMasterUsed(master.masterType, master.name)
            if (inUse) {
                // In use: Deactivate instead
                repository.updateMaster(master.copy(isActive = false, modifiedDate = System.currentTimeMillis()))
                onResult(false)
            } else {
                // Not in use: Soft delete
                repository.softDeleteMaster(master.id)
                onResult(true)
            }
        }
    }
}

class MasterViewModelFactory(private val application: Application, private val repository: MasterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MasterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MasterViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
