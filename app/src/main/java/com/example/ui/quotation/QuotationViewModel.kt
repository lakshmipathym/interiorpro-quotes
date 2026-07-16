package com.example.ui.quotation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.QuotationCalculationEngine
import com.example.engine.TaxEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class QuotationViewModel(application: Application, val repository: QuotesRepository) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            try {
                val existing = repository.allTemplates.first()
                if (existing.size < 5) {
                    val standardKitchenItems = """
                        [
                          {"description":"Base Cabinets (Standard)","quantity":10.0,"unit":"Rft (Running Foot)","rate":1850.0},
                          {"description":"Wall Cabinets (Standard)","quantity":10.0,"unit":"Rft (Running Foot)","rate":1250.0},
                          {"description":"Hettich Hinges Pack","quantity":1.0,"unit":"Sets","rate":3500.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val premiumKitchenItems = """
                        [
                          {"description":"Base Cabinets (Premium)","quantity":12.0,"unit":"Rft (Running Foot)","rate":2500.0},
                          {"description":"Wall Cabinets (Premium)","quantity":12.0,"unit":"Rft (Running Foot)","rate":1800.0},
                          {"description":"Hafele Soft-Close Pack","quantity":1.0,"unit":"Sets","rate":6500.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val budgetWardrobeItems = """
                        [
                          {"description":"Wardrobe Carcass (Budget MDF)","quantity":40.0,"unit":"Sq.Ft","rate":900.0},
                          {"description":"Hinged Shutters","quantity":40.0,"unit":"Sq.Ft","rate":650.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val premiumWardrobeItems = """
                        [
                          {"description":"Wardrobe Carcass (Plywood)","quantity":45.0,"unit":"Sq.Ft","rate":1200.0},
                          {"description":"PU Painted Shutters","quantity":45.0,"unit":"Sq.Ft","rate":1100.0},
                          {"description":"Hafele Sliding System","quantity":1.0,"unit":"Nos","rate":8500.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val alumWardrobeItems = """
                        [
                          {"description":"Aluminium Section Frame Wardrobe","quantity":40.0,"unit":"Sq.Ft","rate":1400.0},
                          {"description":"Glass/ACP Insert Panels","quantity":40.0,"unit":"Sq.Ft","rate":800.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val slidingWardrobeItems = """
                        [
                          {"description":"Sliding Door Wardrobe Carcass","quantity":45.0,"unit":"Sq.Ft","rate":1150.0},
                          {"description":"Sliding Shutters (High Gloss)","quantity":45.0,"unit":"Sq.Ft","rate":950.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val tvUnitItems = """
                        [
                          {"description":"TV Back Panel","quantity":32.0,"unit":"Sq.Ft","rate":450.0},
                          {"description":"Base Drawer Console","quantity":6.0,"unit":"Rft (Running Foot)","rate":1200.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val crockeryUnitItems = """
                        [
                          {"description":"Crockery Cabinet Carcass","quantity":1.0,"unit":"Nos","rate":8500.0},
                          {"description":"Glass Profile Doors","quantity":2.0,"unit":"Nos","rate":2800.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val vanityItems = """
                        [
                          {"description":"Under-Sink Moisture Resistant Cabinet","quantity":1.0,"unit":"Nos","rate":5500.0},
                          {"description":"Mirror Frame Unit","quantity":1.0,"unit":"Nos","rate":2200.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val officeFurnitureItems = """
                        [
                          {"description":"Office Workstation Desk","quantity":2.0,"unit":"Nos","rate":4500.0},
                          {"description":"Pedestal Drawer Unit","quantity":2.0,"unit":"Nos","rate":2500.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val customProjectItems = """
                        [
                          {"description":"Custom Wall Panelling","quantity":100.0,"unit":"Sq.Ft","rate":350.0},
                          {"description":"Custom Design Partition","quantity":1.0,"unit":"Nos","rate":15000.0}
                        ]
                    """.trimIndent().replace("\n", "").replace(" ", "")

                    val presets = listOf(
                        QuotationTemplate(name = "Standard Modular Kitchen", projectType = "Modular Kitchen", category = "Base Cabinets", material = "BWP Plywood", finish = "Matte Laminate", description = "Standard Kitchen Preset", itemsJson = standardKitchenItems),
                        QuotationTemplate(name = "Premium Modular Kitchen", projectType = "Modular Kitchen", category = "Base Cabinets", material = "MDF (Exterior Grade)", finish = "Acrylic Finish", description = "Premium Kitchen Preset", itemsJson = premiumKitchenItems),
                        QuotationTemplate(name = "Budget Wardrobe", projectType = "Wardrobe", category = "Shutters", material = "Particle Board", finish = "Matte Laminate", description = "Budget Wardrobe Preset", itemsJson = budgetWardrobeItems),
                        QuotationTemplate(name = "Premium Wardrobe", projectType = "Wardrobe", category = "Shutters", material = "BWP Plywood", finish = "PU Paint", description = "Premium Wardrobe Preset", itemsJson = premiumWardrobeItems),
                        QuotationTemplate(name = "Aluminium Wardrobe", projectType = "Wardrobe", category = "Shutters", material = "Aluminium Section Framework", finish = "Powder Coated", description = "Aluminium Wardrobe Preset", itemsJson = alumWardrobeItems),
                        QuotationTemplate(name = "Sliding Wardrobe", projectType = "Wardrobe", category = "Shutters", material = "BWP Plywood", finish = "High Gloss Laminate", description = "Sliding Wardrobe Preset", itemsJson = slidingWardrobeItems),
                        QuotationTemplate(name = "TV Unit", projectType = "Living Room TV Unit", category = "Base Cabinets", material = "HDF", finish = "High Gloss Laminate", description = "TV Unit Preset", itemsJson = tvUnitItems),
                        QuotationTemplate(name = "Crockery Unit", projectType = "Living Room TV Unit", category = "Base Cabinets", material = "BWP Plywood", finish = "Matte Laminate", description = "Crockery Unit Preset", itemsJson = crockeryUnitItems),
                        QuotationTemplate(name = "Vanity", projectType = "Full Home Interior", category = "Base Cabinets", material = "BWP Plywood", finish = "PU Paint", description = "Bathroom Vanity Preset", itemsJson = vanityItems),
                        QuotationTemplate(name = "Office Furniture", projectType = "Office Workstations", category = "Base Cabinets", material = "Particle Board", finish = "Matte Laminate", description = "Office Workstations Preset", itemsJson = officeFurnitureItems),
                        QuotationTemplate(name = "Custom Project", projectType = "Full Home Interior", category = "Base Cabinets", material = "BWP Plywood", finish = "High Gloss Laminate", description = "Custom Project Preset", itemsJson = customProjectItems)
                    )

                    presets.forEach {
                        repository.saveTemplate(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val allMasterData: StateFlow<List<MasterData>> = repository.allMasterData
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val allTemplates: StateFlow<List<QuotationTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companyProfile: StateFlow<CompanyProfile?> = repository.companyProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _editingQuotationId = MutableStateFlow<Int?>(null)
    val editingQuotationId: StateFlow<Int?> = _editingQuotationId.asStateFlow()

    private val _editingQuotationStatus = MutableStateFlow("Draft")
    val editingQuotationStatus: StateFlow<String> = _editingQuotationStatus.asStateFlow()

    private val _newQuoteCustomer = MutableStateFlow<CustomerEntity?>(null)
    val newQuoteCustomer: StateFlow<CustomerEntity?> = _newQuoteCustomer.asStateFlow()

    private val _newQuoteProjectType = MutableStateFlow("")
    val newQuoteProjectType: StateFlow<String> = _newQuoteProjectType.asStateFlow()

    private val _newQuoteCategory = MutableStateFlow("")
    val newQuoteCategory: StateFlow<String> = _newQuoteCategory.asStateFlow()

    private val _newQuoteMaterial = MutableStateFlow("")
    val newQuoteMaterial: StateFlow<String> = _newQuoteMaterial.asStateFlow()

    private val _newQuoteFinish = MutableStateFlow("")
    val newQuoteFinish: StateFlow<String> = _newQuoteFinish.asStateFlow()

    private val _newQuoteTemplate = MutableStateFlow<QuotationTemplate?>(null)
    val newQuoteTemplate: StateFlow<QuotationTemplate?> = _newQuoteTemplate.asStateFlow()

    private val _newQuoteItems = MutableStateFlow<List<QuotationItem>>(emptyList())
    val newQuoteItems: StateFlow<List<QuotationItem>> = _newQuoteItems.asStateFlow()

    private val _newQuoteDiscount = MutableStateFlow(0.0)
    val newQuoteDiscount: StateFlow<Double> = _newQuoteDiscount.asStateFlow()

    private val _newQuoteGstRate = MutableStateFlow(18.0)
    val newQuoteGstRate: StateFlow<Double> = _newQuoteGstRate.asStateFlow()

    private val _newQuoteTerms = MutableStateFlow("")
    val newQuoteTerms: StateFlow<String> = _newQuoteTerms.asStateFlow()

    private val _newQuoteWarranty = MutableStateFlow("")
    val newQuoteWarranty: StateFlow<String> = _newQuoteWarranty.asStateFlow()

    private val _newQuoteNumber = MutableStateFlow("")
    val newQuoteNumber: StateFlow<String> = _newQuoteNumber.asStateFlow()

    val newQuoteSubtotal: StateFlow<Double> = _newQuoteItems.map { items ->
        QuotationCalculationEngine.calculateSubtotal(items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val newQuoteGstAmount: StateFlow<Double> = combine(newQuoteSubtotal, _newQuoteDiscount, _newQuoteGstRate) { sub, disc, rate ->
        TaxEngine.calculateGstAmount(sub, disc, rate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val newQuoteGrandTotal: StateFlow<Double> = combine(newQuoteSubtotal, _newQuoteDiscount, newQuoteGstAmount) { sub, disc, gst ->
        QuotationCalculationEngine.calculateGrandTotal(sub, disc, gst)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun startNewQuotation() {
        viewModelScope.launch {
            _editingQuotationId.value = null
            _editingQuotationStatus.value = "Draft"
            _newQuoteCustomer.value = null
            _newQuoteProjectType.value = ""
            _newQuoteCategory.value = ""
            _newQuoteMaterial.value = ""
            _newQuoteFinish.value = ""
            _newQuoteTemplate.value = null
            _newQuoteItems.value = emptyList()
            
            val company = repository.getCompanyProfileDirect()
            _newQuoteDiscount.value = company?.defaultDiscount ?: 0.0
            _newQuoteGstRate.value = company?.defaultGstRate ?: 18.0
            _newQuoteWarranty.value = ""
            
            _newQuoteTerms.value = ""
            _newQuoteNumber.value = repository.generateNextQuotationNumber()
        }
    }

    fun selectCustomer(customer: CustomerEntity) {
        _newQuoteCustomer.value = customer
    }

    fun selectProjectType(type: String) {
        _newQuoteProjectType.value = type
    }

    fun selectCategory(category: String) {
        _newQuoteCategory.value = category
    }

    fun selectMaterial(material: String) {
        _newQuoteMaterial.value = material
    }

    fun selectFinish(finish: String) {
        _newQuoteFinish.value = finish
    }

    fun selectTemplate(template: QuotationTemplate?) {
        _newQuoteTemplate.value = template
        if (template != null) {
            if (template.projectType.isNotEmpty()) _newQuoteProjectType.value = template.projectType
            if (template.category.isNotEmpty()) _newQuoteCategory.value = template.category
            if (template.material.isNotEmpty()) _newQuoteMaterial.value = template.material
            if (template.finish.isNotEmpty()) _newQuoteFinish.value = template.finish
            
            try {
                val array = org.json.JSONArray(template.itemsJson)
                val items = mutableListOf<QuotationItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val desc = obj.getString("description")
                    val qty = obj.getDouble("quantity")
                    val unit = obj.getString("unit")
                    val rate = obj.getDouble("rate")
                    items.add(
                        QuotationItem(
                            quotationId = 0,
                            description = desc,
                            quantity = qty,
                            unit = unit,
                            rate = rate,
                            amount = qty * rate
                        )
                    )
                }
                _newQuoteItems.value = items
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setQuoteItems(items: List<QuotationItem>) {
        _newQuoteItems.value = items
    }

    fun addQuoteItem(item: QuotationItem) {
        _newQuoteItems.value = _newQuoteItems.value + item
    }

    fun removeQuoteItem(index: Int) {
        val current = _newQuoteItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _newQuoteItems.value = current
        }
    }

    fun updateQuoteItem(index: Int, updated: QuotationItem) {
        val current = _newQuoteItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _newQuoteItems.value = current
        }
    }

    fun duplicateQuoteItem(index: Int) {
        val current = _newQuoteItems.value.toMutableList()
        if (index in current.indices) {
            val original = current[index]
            val duplicated = original.copy(id = 0)
            current.add(index + 1, duplicated)
            _newQuoteItems.value = current
        }
    }

    fun setDiscount(disc: Double) {
        _newQuoteDiscount.value = disc
    }

    fun setGstRate(rate: Double) {
        _newQuoteGstRate.value = rate
    }

    fun setTerms(terms: String) {
        _newQuoteTerms.value = terms
    }

    fun setWarranty(warranty: String) {
        _newQuoteWarranty.value = warranty
    }

    fun setQuoteNumber(num: String) {
        _newQuoteNumber.value = num
    }

    fun saveQuotation(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val customer = _newQuoteCustomer.value ?: return@launch
            val quote = Quotation(
                id = _editingQuotationId.value ?: 0,
                quotationNumber = _newQuoteNumber.value.ifEmpty { repository.generateNextQuotationNumber() },
                customerId = customer.customerId.toInt(),
                customerName = customer.customerName,
                customerPhone = customer.mobileNumber,
                customerAddress = customer.address,
                projectType = _newQuoteProjectType.value,
                category = _newQuoteCategory.value,
                material = _newQuoteMaterial.value,
                finish = _newQuoteFinish.value,
                subtotal = newQuoteSubtotal.value,
                discount = _newQuoteDiscount.value,
                gstRate = _newQuoteGstRate.value,
                gstAmount = newQuoteGstAmount.value,
                grandTotal = newQuoteGrandTotal.value,
                termsAndConditions = _newQuoteTerms.value,
                warranty = _newQuoteWarranty.value,
                status = _editingQuotationStatus.value
            )
            val qId = repository.saveQuotationWithItems(quote, _newQuoteItems.value)
            onComplete(qId)
        }
    }

    fun loadQuotationToEdit(quotation: Quotation) {
        viewModelScope.launch {
            _editingQuotationId.value = quotation.id
            _editingQuotationStatus.value = quotation.status
            _newQuoteNumber.value = quotation.quotationNumber
            _newQuoteProjectType.value = quotation.projectType
            _newQuoteCategory.value = quotation.category
            _newQuoteMaterial.value = quotation.material
            _newQuoteFinish.value = quotation.finish
            _newQuoteDiscount.value = quotation.discount
            _newQuoteGstRate.value = quotation.gstRate
            _newQuoteTerms.value = quotation.termsAndConditions
            _newQuoteWarranty.value = quotation.warranty
            
            val cust = repository.getCustomerById(quotation.customerId.toLong())
            _newQuoteCustomer.value = cust ?: CustomerEntity(
                customerId = quotation.customerId.toLong(),
                customerName = quotation.customerName,
                mobileNumber = quotation.customerPhone,
                address = quotation.customerAddress
            )

            val items = repository.getQuotationItemsDirect(quotation.id)
            _newQuoteItems.value = items
        }
    }

    suspend fun getQuotationByIdDirect(id: Int): Quotation? {
        return repository.getQuotationByIdDirect(id)
    }

    suspend fun getQuotationItemsDirect(id: Int): List<QuotationItem> {
        return repository.getQuotationItemsDirect(id)
    }
}

class QuotationViewModelFactory(private val application: Application, private val repository: QuotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuotationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuotationViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
