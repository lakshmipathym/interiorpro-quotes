import re

with open('app/src/test/java/com/example/QuotationEngineTest.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "val finalizeUseCase = FinalizeQuotationUseCase(snapFactory, snapRepo)" in line:
        new_lines.append("        val assetCopier = BrandingAssetCopierImpl(app)\n")
        new_lines.append("        val finalizeUseCase = FinalizeQuotationUseCase(snapFactory, snapRepo, assetCopier)\n")
    elif "val itemEngine = com.example.domain.engine.ItemCalculationEngineImpl" in line:
        skip = True
    elif "val historyViewModel = com.example.ui.history.HistoryViewModel(app, repository, calcUseCase, finalizeUseCase)" in line:
        new_lines.append("        val historyViewModel = com.example.ui.history.HistoryViewModel(app, repository, calcUseCase, finalizeUseCase)\n")
        skip = False
    else:
        if not skip:
            new_lines.append(line)

with open('app/src/test/java/com/example/QuotationEngineTest.kt', 'w') as f:
    f.writelines(new_lines)
