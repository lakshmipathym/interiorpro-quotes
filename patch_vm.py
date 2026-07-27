import sys

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "r") as f:
    content = f.read()

# Add imports
imports_to_add = """
import com.example.domain.usecases.CalculateQuotationUseCase
import com.example.domain.usecases.FinalizeQuotationUseCase
import com.example.domain.contracts.QuotationSnapshotRepository
import com.example.domain.models.*
"""
content = content.replace("import com.example.engine.TaxEngine\n", "import com.example.engine.TaxEngine\n" + imports_to_add)

# Change constructor of ViewModel
old_vm_ctor = """class QuotationViewModel(
    application: Application,
    val repository: QuotesRepository, val masterRepository: com.example.data.MasterRepository,
    private val syncManager: com.example.core.sync.SyncManager
) : AndroidViewModel(application) {"""

new_vm_ctor = """class QuotationViewModel(
    application: Application,
    val repository: QuotesRepository, val masterRepository: com.example.data.MasterRepository,
    private val syncManager: com.example.core.sync.SyncManager,
    private val calculateQuotationUseCase: CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: FinalizeQuotationUseCase,
    private val snapshotRepository: QuotationSnapshotRepository
) : AndroidViewModel(application) {"""

content = content.replace(old_vm_ctor, new_vm_ctor)

# Change factory constructor
old_fac_ctor = """class QuotationViewModelFactory(
    private val application: Application,
    private val repository: QuotesRepository,
    private val masterRepository: com.example.data.MasterRepository,
    private val syncManager: com.example.core.sync.SyncManager
) : ViewModelProvider.Factory {"""

new_fac_ctor = """class QuotationViewModelFactory(
    private val application: Application,
    private val repository: QuotesRepository,
    private val masterRepository: com.example.data.MasterRepository,
    private val syncManager: com.example.core.sync.SyncManager,
    private val calculateQuotationUseCase: CalculateQuotationUseCase,
    private val finalizeQuotationUseCase: FinalizeQuotationUseCase,
    private val snapshotRepository: QuotationSnapshotRepository
) : ViewModelProvider.Factory {"""
content = content.replace(old_fac_ctor, new_fac_ctor)

# Change factory return
old_fac_ret = "return QuotationViewModel(application, repository, masterRepository, syncManager) as T"
new_fac_ret = "return QuotationViewModel(application, repository, masterRepository, syncManager, calculateQuotationUseCase, finalizeQuotationUseCase, snapshotRepository) as T"
content = content.replace(old_fac_ret, new_fac_ret)

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "w") as f:
    f.write(content)
