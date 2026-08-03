import re

with open('app/src/main/java/com/example/domain/usecases/FinalizeQuotationUseCase.kt', 'r') as f:
    content = f.read()

content = content.replace("println(\"Calling copyAssets\"); val updatedCompany = assetCopier?.copyAssetsForQuotation(quotationNumber, company) ?: company; println(\"Finished copyAssets\")", "val updatedCompany = assetCopier?.copyAssetsForQuotation(quotationNumber, company) ?: company")
content = content.replace("println(\"Calling createSnapshot\"); val snapshot = snapshotFactory.createSnapshot(", "val snapshot = snapshotFactory.createSnapshot(")
content = content.replace("println(\"Calling saveSnapshot\"); snapshotRepository.saveSnapshot(snapshot); println(\"Finished saveSnapshot\")", "snapshotRepository.saveSnapshot(snapshot)")

with open('app/src/main/java/com/example/domain/usecases/FinalizeQuotationUseCase.kt', 'w') as f:
    f.write(content)
