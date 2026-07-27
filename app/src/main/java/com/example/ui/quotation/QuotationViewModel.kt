package com.example.ui.quotation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.QuotationCalculationEngine
import com.example.engine.TaxEngine

import com.example.domain.usecases.CalculateQuotationUseCase
import com.example.domain.usecases.FinalizeQuotationUseCase
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class QuotationViewModel(
    application: Application,
    val repository: QuotesRepository, val masterRepository: com.example.data.MasterRepository,
    private val syncManager: com.example.core.sync.SyncManager,
    private val calculateQuotationUseCase: CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: FinalizeQuotationUseCase,
    private val snapshotRepository: QuotationSnapshotRepository
) : AndroidViewModel(application) {

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
            }
        }
    }

    val allMasterData: StateFlow<List<com.example.data.MasterEntity>> = masterRepository.getAllMasters()
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
    private val _newQuoteSiteName = MutableStateFlow("")
    val newQuoteSiteName: StateFlow<String> = _newQuoteSiteName.asStateFlow()
    private val _newQuoteSiteAddress = MutableStateFlow("")
    val newQuoteSiteAddress: StateFlow<String> = _newQuoteSiteAddress.asStateFlow()

    private val _newQuoteProjectType = MutableStateFlow("")
    val newQuoteProjectType: StateFlow<String> = _newQuoteProjectType.asStateFlow()

    private val _newQuoteCategory = MutableStateFlow("")
    val newQuoteCategory: StateFlow<String> = _newQuoteCategory.asStateFlow()

    private val _newQuoteMaterial = MutableStateFlow("")
    val newQuoteMaterial: StateFlow<String> = _newQuoteMaterial.asStateFlow()

    private val _newQuoteFinish = MutableStateFlow("")
    val newQuoteFinish: StateFlow<String> = _newQuoteFinish.asStateFlow()

    private val _newQuoteProjectName = MutableStateFlow("")
    val newQuoteProjectName: StateFlow<String> = _newQuoteProjectName.asStateFlow()

    private val _newQuoteDate = MutableStateFlow(System.currentTimeMillis())
    val newQuoteDate: StateFlow<Long> = _newQuoteDate.asStateFlow()

    private val _newQuoteValidityDays = MutableStateFlow(30)
    val newQuoteValidityDays: StateFlow<Int> = _newQuoteValidityDays.asStateFlow()

    private val _newQuoteTransport = MutableStateFlow(0.0)
    val newQuoteTransport: StateFlow<Double> = _newQuoteTransport.asStateFlow()

    private val _newQuoteInstallation = MutableStateFlow(0.0)
    val newQuoteInstallation: StateFlow<Double> = _newQuoteInstallation.asStateFlow()

    private val _newQuoteExtraCharges = MutableStateFlow(0.0)
    val newQuoteExtraCharges: StateFlow<Double> = _newQuoteExtraCharges.asStateFlow()

    private val _newQuoteRoundOff = MutableStateFlow(0.0)
    val newQuoteRoundOff: StateFlow<Double> = _newQuoteRoundOff.asStateFlow()

    private val _newQuoteAdvance = MutableStateFlow(0.0)
    val newQuoteAdvance: StateFlow<Double> = _newQuoteAdvance.asStateFlow()

    private val _newQuoteCustomerNotes = MutableStateFlow("")
    val newQuoteCustomerNotes: StateFlow<String> = _newQuoteCustomerNotes.asStateFlow()

    private val _newQuoteInternalNotes = MutableStateFlow("")
    val newQuoteInternalNotes: StateFlow<String> = _newQuoteInternalNotes.asStateFlow()

    private val _newQuoteTemplate = MutableStateFlow<QuotationTemplate?>(null)
    val newQuoteTemplate: StateFlow<QuotationTemplate?> = _newQuoteTemplate.asStateFlow()

    private val _newQuoteItems = MutableStateFlow<List<QuotationItem>>(emptyList())
    
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

    val calculatedQuotation: StateFlow<CalculatedQuotation> = combine(
        combine(_newQuoteItems, _newQuoteDiscount, _newQuoteGstRate) { i, d, g -> Triple(i, d, g) },
        combine(_newQuoteTransport, _newQuoteInstallation, _newQuoteExtraCharges) { t, i, e -> Triple(t, i, e) },
        combine(_newQuoteRoundOff, _newQuoteAdvance) { r, a -> Pair(r, a) }
    ) { (items, discount, gstRate), (transport, installation, extraCharges), (roundOff, advance) ->
        val rawInput = RawQuotationInput(
            discount = discount,
            gstRate = gstRate,
            transport = transport,
            installation = installation,
            extraCharges = extraCharges,
            roundOff = roundOff,
            advance = advance
        )
        val rawItems = items.map {
            val parts = it.description.split("|||")
            val userDesc = parts[0].trim()
            val specsJson = if (parts.size > 1) parts[1].trim() else "{}"
            var w = "0"; var h = "0"; var d = "0"
            var jsonParsed = false
            try {
                if (specsJson.startsWith("{") && specsJson.endsWith("}")) {
                    val json = org.json.JSONObject(specsJson)
                    if (json.has("width") || json.has("height") || json.has("depth")) {
                        w = json.optString("width", "0")
                        h = json.optString("height", "0")
                        d = json.optString("depth", "0")
                        jsonParsed = true
                    }
                }
            } catch (e: Exception) {}

            if (!jsonParsed || (w == "0" && h == "0" && d == "0")) {
                if (it.rawWidth.isNotEmpty()) w = it.rawWidth
                if (it.rawHeight.isNotEmpty()) h = it.rawHeight
                if (it.rawDepth.isNotEmpty()) d = it.rawDepth
            }

            val qty = if (it.rawQuantity > 0.0 && it.quantity == it.billableQuantity) {
                it.rawQuantity
            } else {
                it.quantity
            }

            RawItemInput(
                itemName = it.itemName,
                description = userDesc,
                material = it.material,
                finish = it.finish,
                width = w,
                height = h,
                depth = d,
                quantity = qty,
                unit = it.unit,
                rate = it.rate
            )
        }
        calculateQuotationUseCase.execute(rawInput, rawItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
        CalculatedQuotation(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, "")
    )

    val newQuoteSubtotal: StateFlow<Double> = calculatedQuotation.map { it.subtotal }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val newQuoteGstAmount: StateFlow<Double> = calculatedQuotation.map { it.gstAmount }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val newQuoteGrandTotal: StateFlow<Double> = calculatedQuotation.map { it.grandTotal }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val newQuoteBalance: StateFlow<Double> = calculatedQuotation.map { it.balanceDue }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val newQuoteItems: StateFlow<List<QuotationItem>> = combine(_newQuoteItems, calculatedQuotation) { raw, calc ->
        if (raw.size == calc.items.size) {
            raw.mapIndexed { index, item ->
                item.copy(
                    billableQuantity = calc.items[index].billableQuantity,
                    amount = calc.items[index].itemAmount
                )
            }
        } else {
            raw
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startNewQuotation() {
        viewModelScope.launch {
            _editingQuotationId.value = null
            _editingQuotationStatus.value = "Draft"
            _newQuoteCustomer.value = null
            _newQuoteSiteName.value = ""
            _newQuoteSiteAddress.value = ""
            _newQuoteProjectName.value = ""
            _newQuoteDate.value = System.currentTimeMillis()
            _newQuoteProjectType.value = ""
            _newQuoteCategory.value = ""
            _newQuoteMaterial.value = ""
            _newQuoteFinish.value = ""
            _newQuoteTemplate.value = null
            _newQuoteItems.value = emptyList()
            
            val company = repository.getCompanyProfileDirect()
            _newQuoteValidityDays.value = company?.defaultValidityDays?.takeIf { it > 0 } ?: 30
            _newQuoteDiscount.value = company?.defaultDiscount ?: 0.0
            _newQuoteGstRate.value = company?.defaultGstRate ?: 18.0
            _newQuoteWarranty.value = company?.defaultWarranty ?: ""
            _newQuoteTerms.value = company?.termsAndConditions ?: ""
            
            _newQuoteTransport.value = 0.0
            _newQuoteInstallation.value = 0.0
            _newQuoteExtraCharges.value = 0.0
            _newQuoteRoundOff.value = 0.0
            _newQuoteAdvance.value = 0.0
            _newQuoteCustomerNotes.value = ""
            _newQuoteInternalNotes.value = ""
            
            _newQuoteNumber.value = repository.generateNextQuotationNumber()
        }
    }

    fun selectCustomer(customer: CustomerEntity) {
        _newQuoteCustomer.value = customer
        _newQuoteSiteName.value = customer.siteLocation
        _newQuoteSiteAddress.value = customer.siteAddress.ifBlank { customer.address }
    }

    fun updateSiteDetails(name: String, address: String) {
        _newQuoteSiteName.value = name
        _newQuoteSiteAddress.value = address
    }

    fun previewItemCalculation(width: String, height: String, depth: String, qty: Double, unit: String, rate: Double): Pair<Double, Double> {
        val rawItem = com.example.domain.models.RawItemInput(
            itemName = "", description = "", material = "", finish = "",
            width = width, height = height, depth = depth, quantity = qty, unit = unit, rate = rate
        )
        val calculated = calculateQuotationUseCase.previewItem(rawItem)
        return Pair(calculated.billableQuantity, calculated.itemAmount)
    }

    fun updateProjectName(name: String) {
        _newQuoteProjectName.value = name
    }

    fun updateDate(dateMillis: Long) {
        _newQuoteDate.value = dateMillis
    }

    fun updateValidityDays(days: Int) {
        _newQuoteValidityDays.value = days
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


    fun moveQuoteItemUp(index: Int) {
        if (index > 0) {
            val list = _newQuoteItems.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index - 1, item)
            _newQuoteItems.value = list
        }
    }

    fun moveQuoteItemDown(index: Int) {
        if (index < _newQuoteItems.value.size - 1) {
            val list = _newQuoteItems.value.toMutableList()
            val item = list.removeAt(index)
            list.add(index + 1, item)
            _newQuoteItems.value = list
        }
    }

    fun duplicateQuoteItem(index: Int) {
        val current = _newQuoteItems.value.toMutableList()
        if (index in current.indices) {
            val original = current[index]
            var duplicatedDesc = original.description
            try {
                if (duplicatedDesc.contains("|||")) {
                    val parts = duplicatedDesc.split("|||")
                    val userDesc = parts[0].trim()
                    val specsJson = parts[1].trim()
                    if (specsJson.startsWith("{") && specsJson.endsWith("}")) {
                        val json = org.json.JSONObject(specsJson)
                        
                        val laminateImageUri = json.optString("laminateImageUri", "")
                        if (laminateImageUri.isNotEmpty()) {
                            val filesDir = getApplication<Application>().filesDir
                            val oldFile = java.io.File(filesDir, java.io.File(laminateImageUri).name)
                            if (oldFile.exists()) {
                                val newFile = java.io.File(oldFile.parent, "temp_lam_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0,5)}.jpg")
                                oldFile.copyTo(newFile, overwrite = true)
                                json.put("laminateImageUri", newFile.absolutePath)
                            }
                        }
                        
                        val designImageUri = json.optString("designImageUri", "")
                        if (designImageUri.isNotEmpty()) {
                            val filesDir = getApplication<Application>().filesDir
                            val oldFile = java.io.File(filesDir, java.io.File(designImageUri).name)
                            if (oldFile.exists()) {
                                val newFile = java.io.File(oldFile.parent, "temp_des_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0,5)}.jpg")
                                oldFile.copyTo(newFile, overwrite = true)
                                json.put("designImageUri", newFile.absolutePath)
                            }
                        }
                        
                        duplicatedDesc = "$userDesc ||| $json"
                    }
                } else if (duplicatedDesc.startsWith("{") && duplicatedDesc.endsWith("}")) {
                    val json = org.json.JSONObject(duplicatedDesc)
                    
                    val laminateImageUri = json.optString("laminateImageUri", "")
                    if (laminateImageUri.isNotEmpty()) {
                        val filesDir = getApplication<Application>().filesDir
                            val oldFile = java.io.File(filesDir, java.io.File(laminateImageUri).name)
                        if (oldFile.exists()) {
                            val newFile = java.io.File(oldFile.parent, "temp_lam_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0,5)}.jpg")
                            oldFile.copyTo(newFile, overwrite = true)
                            json.put("laminateImageUri", newFile.absolutePath)
                        }
                    }
                    
                    val designImageUri = json.optString("designImageUri", "")
                    if (designImageUri.isNotEmpty()) {
                        val filesDir = getApplication<Application>().filesDir
                            val oldFile = java.io.File(filesDir, java.io.File(designImageUri).name)
                        if (oldFile.exists()) {
                            val newFile = java.io.File(oldFile.parent, "temp_des_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().substring(0,5)}.jpg")
                            oldFile.copyTo(newFile, overwrite = true)
                            json.put("designImageUri", newFile.absolutePath)
                        }
                    }
                    
                    duplicatedDesc = json.toString()
                }
            } catch (e: Exception) {
                // Ignore
            }
            val duplicated = original.copy(id = 0, description = duplicatedDesc)
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

    fun setTransport(value: Double) {
        _newQuoteTransport.value = value
    }

    fun setInstallation(value: Double) {
        _newQuoteInstallation.value = value
    }

    fun setExtraCharges(value: Double) {
        _newQuoteExtraCharges.value = value
    }

    fun setRoundOff(value: Double) {
        _newQuoteRoundOff.value = value
    }

    fun setAdvance(value: Double) {
        _newQuoteAdvance.value = value
    }

    fun setCustomerNotes(notes: String) {
        _newQuoteCustomerNotes.value = notes
    }

    fun setInternalNotes(notes: String) {
        _newQuoteInternalNotes.value = notes
    }

    fun setQuoteNumber(num: String) {
        _newQuoteNumber.value = num
    }

    
    fun validateStep(step: Int): String? {
        return when (step) {
            1 -> {
                when {
                    _newQuoteCustomer.value == null -> "Select a customer to continue"
                    _newQuoteSiteName.value.isBlank() -> "Enter Site Name to continue"
                    _newQuoteSiteAddress.value.isBlank() -> "Enter Site Address to continue"
                    else -> null
                }
            }
            2 -> {
                when {
                    _newQuoteItems.value.isEmpty() -> "Add at least one item to continue"
                    else -> null
                }
            }
            3 -> {
                when {
                    _newQuoteDiscount.value > newQuoteSubtotal.value -> "Discount cannot exceed subtotal"
                    _newQuoteGstRate.value < 0 -> "GST rate cannot be negative"
                    _newQuoteTransport.value < 0 -> "Transport cannot be negative"
                    _newQuoteInstallation.value < 0 -> "Installation cannot be negative"
                    _newQuoteExtraCharges.value < 0 -> "Extra charges cannot be negative"
                    _newQuoteAdvance.value < 0 -> "Advance cannot be negative"
                    else -> null
                }
            }
            else -> null
        }
    }

    fun saveQuotation(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val customerEntity = _newQuoteCustomer.value ?: return@launch
            val companyProfile = repository.getCompanyProfileDirect() ?: return@launch
            
            val quoteNumber = _newQuoteNumber.value.ifEmpty { repository.generateNextQuotationNumber() }
            val qIdStr = (_editingQuotationId.value ?: 0).toString()

            val customerSnapshot = CustomerSnapshot(
                customerId = customerEntity.customerId.toString(),
                customerName = customerEntity.customerName,
                customerPhone = customerEntity.mobileNumber,
                customerAddress = customerEntity.address,
                siteName = _newQuoteSiteName.value,
                siteAddress = _newQuoteSiteAddress.value
            )

            val companySnapshot = CompanySnapshot(
                companyName = companyProfile.companyName,
                ownerName = companyProfile.ownerName,
                phone = companyProfile.phone,
                email = companyProfile.email,
                address = companyProfile.address,
                gstin = companyProfile.gstin,
                bankName = companyProfile.bankName,
                accountHolderName = companyProfile.accountHolderName,
                accountNumber = companyProfile.accountNumber,
                ifsc = companyProfile.ifsc,
                branch = companyProfile.branch,
                upiId = companyProfile.upiId
            )

            val rawInput = RawQuotationInput(
                discount = _newQuoteDiscount.value,
                gstRate = _newQuoteGstRate.value,
                transport = _newQuoteTransport.value,
                installation = _newQuoteInstallation.value,
                extraCharges = _newQuoteExtraCharges.value,
                roundOff = _newQuoteRoundOff.value,
                advance = _newQuoteAdvance.value
            )
            
            val calcQuote = calculatedQuotation.value

            finalizeQuotationUseCase.execute(
                id = qIdStr,
                quotationNumber = quoteNumber,
                date = _newQuoteDate.value,
                customer = customerSnapshot,
                company = companySnapshot,
                termsAndConditions = _newQuoteTerms.value,
                warranty = _newQuoteWarranty.value,
                validityDays = _newQuoteValidityDays.value,
                notes = _newQuoteCustomerNotes.value,
                rawInput = rawInput,
                calculatedQuotation = calcQuote
            )
            
            // To maintain compatibility with legacy onComplete(Int), fetch the saved snapshot
            val savedSnapshot = snapshotRepository.getSnapshotByNumber(quoteNumber)
            val newId = savedSnapshot?.id?.toIntOrNull() ?: _editingQuotationId.value ?: 0
            
            syncManager.onQuotationSaved()
            onComplete(newId)
        }
    }

    fun loadQuotationToEdit(quotation: Quotation) {
        viewModelScope.launch {
            if (quotation.status.equals("Final", ignoreCase = true) || quotation.status.equals("Cancelled", ignoreCase = true)) {
                _editingQuotationId.value = 0
                _editingQuotationStatus.value = "Draft"
                _newQuoteNumber.value = ""
            } else {
                _editingQuotationId.value = quotation.id
                _editingQuotationStatus.value = quotation.status
                _newQuoteNumber.value = quotation.quotationNumber
            }
            _newQuoteDate.value = quotation.date
            _newQuoteProjectType.value = quotation.projectType
            _newQuoteCategory.value = quotation.category
            _newQuoteMaterial.value = quotation.material
            _newQuoteFinish.value = quotation.finish
            _newQuoteDiscount.value = quotation.discount
            _newQuoteGstRate.value = quotation.gstRate
            _newQuoteTerms.value = quotation.termsAndConditions
            _newQuoteWarranty.value = quotation.warranty
            
            _newQuoteSiteName.value = quotation.siteName
            _newQuoteSiteAddress.value = quotation.siteAddress
            _newQuoteProjectName.value = quotation.projectName
            
            _newQuoteTransport.value = quotation.transport
            _newQuoteInstallation.value = quotation.installation
            _newQuoteExtraCharges.value = quotation.extraCharges
            _newQuoteRoundOff.value = quotation.roundOff
            _newQuoteAdvance.value = quotation.advance
            _newQuoteCustomerNotes.value = quotation.customerNotes
            _newQuoteInternalNotes.value = quotation.internalNotes
            _newQuoteValidityDays.value = quotation.validityDays

            val dbCustomer = repository.getCustomerById(quotation.customerId)
            _newQuoteCustomer.value = dbCustomer ?: CustomerEntity(
                customerId = quotation.customerId,
                customerName = quotation.customerName,
                mobileNumber = quotation.customerPhone,
                address = quotation.customerAddress,
                email = "", city = "", state = "", pincode = "",
                gstin = "", companyName = "", siteAddress = ""
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

class QuotationViewModelFactory(
    private val application: Application,
    private val repository: QuotesRepository,
    private val masterRepository: com.example.data.MasterRepository,
    private val syncManager: com.example.core.sync.SyncManager,
    private val calculateQuotationUseCase: CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: FinalizeQuotationUseCase,
    private val snapshotRepository: QuotationSnapshotRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuotationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuotationViewModel(application, repository, masterRepository, syncManager, calculateQuotationUseCase, finalizeQuotationUseCase, snapshotRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
