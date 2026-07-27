import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add imports
imports_to_add = """
import com.example.domain.usecases.CalculateQuotationUseCase
import com.example.domain.usecases.FinalizeQuotationUseCase
import com.example.domain.engine.ItemCalculationEngineImpl
import com.example.domain.engine.QuotationCalculationEngineImpl
import com.example.domain.engine.DimensionParserImpl
import com.example.domain.engine.AmountInWordsConverterImpl
import com.example.data.snapshot.QuotationSnapshotRepositoryImpl
import com.example.domain.engine.QuotationSnapshotFactoryImpl
"""
content = content.replace("import com.example.ui.quotation.QuotationViewModelFactory\n", "import com.example.ui.quotation.QuotationViewModelFactory\n" + imports_to_add)

factory_args = """
        val itemEngine = ItemCalculationEngineImpl(DimensionParserImpl())
        val calcEngine = QuotationCalculationEngineImpl(AmountInWordsConverterImpl())
        val calcUseCase = CalculateQuotationUseCase(itemEngine, calcEngine)
        val snapFactory = QuotationSnapshotFactoryImpl()
        val snapRepo = QuotationSnapshotRepositoryImpl(com.example.data.AppDatabase.getDatabase(applicationContext), repository)
        val finalizeUseCase = FinalizeQuotationUseCase(snapFactory, snapRepo)
        ViewModelProvider(this, QuotationViewModelFactory(application, repository, masterRepository, syncManager, calcUseCase, finalizeUseCase, snapRepo))[QuotationViewModel::class.java]
"""
content = content.replace("ViewModelProvider(this, QuotationViewModelFactory(application, repository, masterRepository, syncManager))[QuotationViewModel::class.java]", factory_args.strip())

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
