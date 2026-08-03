import re

with open('app/src/main/java/com/example/domain/usecases/FinalizeQuotationUseCase.kt', 'r') as f:
    content = f.read()

content = content.replace("val updatedCompany = assetCopier?.copyAssetsForQuotation(quotationNumber, company) ?: company", "println(\"Calling copyAssets\"); val updatedCompany = assetCopier?.copyAssetsForQuotation(quotationNumber, company) ?: company; println(\"Finished copyAssets\")")
content = content.replace("val snapshot = snapshotFactory.createSnapshot(", "println(\"Calling createSnapshot\"); val snapshot = snapshotFactory.createSnapshot(")
content = content.replace("snapshotRepository.saveSnapshot(snapshot)", "println(\"Calling saveSnapshot\"); snapshotRepository.saveSnapshot(snapshot); println(\"Finished saveSnapshot\")")

with open('app/src/main/java/com/example/domain/usecases/FinalizeQuotationUseCase.kt', 'w') as f:
    f.write(content)
