perl -0777 -pi -e 's/class CompanyViewModelFactory\(\s*private val application: Application,\s*private val repository: QuotesRepository\s*\)/class CompanyViewModelFactory(private val application: Application, private val repository: QuotesRepository, private val masterRepository: com.example.data.MasterRepository)/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt

perl -0777 -pi -e 's/return CompanyViewModel\(application, repository\) as T/return CompanyViewModel(application, repository, masterRepository) as T/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt
