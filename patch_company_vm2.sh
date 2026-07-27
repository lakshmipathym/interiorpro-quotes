perl -0777 -pi -e 's/fun getMasterDataByType\(type: String\): Flow<List<MasterData>> = repository\.getMasterDataByType\(type\)/fun getMasterDataByType(type: String): Flow<List<com.example.data.MasterEntity>> = masterRepository.getMastersByType(type)/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt

perl -0777 -pi -e 's/repository\.saveMasterData\(MasterData\(type = type, value = value, extra = extra\)\)/masterRepository.saveMaster(com.example.data.MasterEntity(masterType = type, name = value, description = extra))/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt

perl -0777 -pi -e 's/fun deleteMasterData\(master: MasterData\)/fun deleteMasterData(master: com.example.data.MasterEntity)/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt

perl -0777 -pi -e 's/repository\.deleteMasterData\(master\)/masterRepository.deleteMasterPermanently(master)/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt

perl -0777 -pi -e 's/fun deleteMasterDataById\(id: Int\)/fun deleteMasterDataById(id: Long)/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt

perl -0777 -pi -e 's/repository\.deleteMasterDataById\(id\)/masterRepository.softDeleteMaster(id)/' app/src/main/java/com/example/ui/company/CompanyViewModel.kt
