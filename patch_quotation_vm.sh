sed -i 's/val repository: QuotesRepository,/val repository: QuotesRepository, val masterRepository: com.example.data.MasterRepository,/' app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt

sed -i 's/val allMasterData: StateFlow<List<MasterData>> = repository.allMasterData/val allMasterData: StateFlow<List<com.example.data.MasterEntity>> = masterRepository.getAllMasters()/' app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt

sed -i 's/private val repository: QuotesRepository,/private val repository: QuotesRepository, private val masterRepository: com.example.data.MasterRepository,/' app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt

