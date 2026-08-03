import re

# 1. Update MainActivity.kt
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    main_content = f.read()

main_old = """    private val historyViewModel: HistoryViewModel by lazy {
        ViewModelProvider(this, HistoryViewModelFactory(application, repository))[HistoryViewModel::class.java]
    }"""
main_new = """    private val historyViewModel: HistoryViewModel by lazy {
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = com.example.domain.usecases.CalculateQuotationUseCase(itemEngine, calcEngine)
        val snapFactory = QuotationSnapshotFactoryImpl()
        val snapRepo = com.example.data.snapshot.QuotationSnapshotRepositoryImpl(com.example.data.AppDatabase.getDatabase(applicationContext), repository)
        val assetCopier = com.example.data.BrandingAssetCopierImpl(applicationContext)
        val finalizeUseCase = com.example.domain.usecases.FinalizeQuotationUseCase(snapFactory, snapRepo, assetCopier)
        ViewModelProvider(this, HistoryViewModelFactory(application, repository, calcUseCase, finalizeUseCase))[HistoryViewModel::class.java]
    }"""

if main_old in main_content:
    main_content = main_content.replace(main_old, main_new)
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(main_content)
    print("Patched MainActivity.kt")
else:
    print("Could not find HistoryViewModel in MainActivity.kt")


# 2. Update HistoryViewModel.kt
with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'r') as f:
    history_content = f.read()

history_old_class = "class HistoryViewModel(application: Application, val repository: QuotesRepository) : AndroidViewModel(application) {"
history_new_class = """class HistoryViewModel(
    application: Application,
    val repository: QuotesRepository,
    private val calculateQuotationUseCase: com.example.domain.usecases.CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: com.example.domain.usecases.FinalizeQuotationUseCase
) : AndroidViewModel(application) {"""
history_content = history_content.replace(history_old_class, history_new_class)

history_old_factory = "class HistoryViewModelFactory(private val application: Application, private val repository: QuotesRepository) : ViewModelProvider.Factory {"
history_new_factory = """class HistoryViewModelFactory(
    private val application: Application,
    private val repository: QuotesRepository,
    private val calculateQuotationUseCase: com.example.domain.usecases.CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: com.example.domain.usecases.FinalizeQuotationUseCase
) : ViewModelProvider.Factory {"""
history_content = history_content.replace(history_old_factory, history_new_factory)

history_old_factory_create = "return HistoryViewModel(application, repository) as T"
history_new_factory_create = "return HistoryViewModel(application, repository, calculateQuotationUseCase, finalizeQuotationUseCase) as T"
history_content = history_content.replace(history_old_factory_create, history_new_factory_create)


history_old_update = """    fun updateQuotationStatus(id: Int, status: String) {
        viewModelScope.launch {
            repository.getQuotationByIdDirect(id)?.let { current ->
                repository.saveQuotationWithItems(
                    current.copy(status = status),
                    repository.getQuotationItemsDirect(id)
                )
            }
        }
    }"""

history_new_update = """    fun updateQuotationStatus(id: Int, status: String) {
        viewModelScope.launch {
            val current = repository.getQuotationByIdDirect(id) ?: return@launch
            if (status.equals("Final", ignoreCase = true) || status.equals("FINALIZED", ignoreCase = true)) {
                if (current.status.equals("Final", ignoreCase = true) || current.status.equals("FINALIZED", ignoreCase = true)) return@launch
                try {
                    val items = repository.getQuotationItemsDirect(id)
                    val customerEntity = repository.getCustomerByIdDirect(current.customerId) ?: return@launch
                    val companyProfile = repository.getCompanyProfileDirect() ?: com.example.data.CompanyProfile()
                    
                    val customerSnapshot = com.example.domain.models.CustomerSnapshot(
                        customerId = customerEntity.customerId.toString(),
                        customerName = customerEntity.customerName,
                        customerPhone = customerEntity.mobileNumber,
                        customerAddress = customerEntity.address,
                        siteName = current.siteName,
                        siteAddress = current.siteAddress
                    )
                    
                    val companySnapshot = com.example.domain.models.CompanySnapshot(
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
                        upiId = companyProfile.upiId,
                        website = companyProfile.website,
                        whatsappNumber = companyProfile.whatsappNumber,
                        logoPath = companyProfile.logoPath,
                        signaturePath = companyProfile.signaturePath,
                        companySealPath = companyProfile.companySealPath
                    )
                    
                    val rawInput = com.example.domain.models.RawQuotationInput(
                        discount = current.discount,
                        gstRate = current.gstRate,
                        transport = current.transport,
                        installation = current.installation,
                        extraCharges = current.extraCharges,
                        roundOff = current.roundOff,
                        advance = current.advance
                    )
                    
                    val rawItems = items.map {
                        val parts = it.description.split("|||")
                        val specsJson = if (parts.size > 1) parts[1].trim() else "{}"
                        var w = "0"; var h = "0"; var d = "0"
                        try {
                            if (specsJson.startsWith("{") && specsJson.endsWith("}")) {
                                val json = org.json.JSONObject(specsJson)
                                w = json.optString("width", "0")
                                h = json.optString("height", "0")
                                d = json.optString("depth", "0")
                            }
                        } catch (e: Exception) {}
                        
                        com.example.domain.models.RawItemInput(
                            itemName = it.itemName,
                            description = it.description,
                            material = it.material,
                            finish = it.finish,
                            width = w,
                            height = h,
                            depth = d,
                            unit = it.unit,
                            quantity = it.quantity,
                            rate = it.rate
                        )
                    }
                    
                    val calculatedQuotation = calculateQuotationUseCase.execute(rawInput, rawItems)
                    
                    finalizeQuotationUseCase.execute(
                        id = current.id.toString(),
                        quotationNumber = current.quotationNumber,
                        date = current.date,
                        customer = customerSnapshot,
                        company = companySnapshot,
                        termsAndConditions = current.termsAndConditions,
                        warranty = current.warranty,
                        validityDays = current.validityDays,
                        notes = current.customerNotes,
                        rawInput = rawInput,
                        calculatedQuotation = calculatedQuotation
                    )
                    
                    repository.saveQuotationWithItems(
                        current.copy(status = "FINALIZED"),
                        items
                    )
                } catch (e: Exception) {
                    // Do not update status on failure
                }
            } else {
                repository.saveQuotationWithItems(
                    current.copy(status = status),
                    repository.getQuotationItemsDirect(id)
                )
            }
        }
    }"""

history_content = history_content.replace(history_old_update, history_new_update)

with open('app/src/main/java/com/example/ui/history/HistoryViewModel.kt', 'w') as f:
    f.write(history_content)
print("Patched HistoryViewModel.kt")

