perl -0777 -pi -e 's/com\.example\.data\.MasterData/com.example.data.MasterEntity/g' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
perl -0777 -pi -e 's/it\.type ==/it.masterType ==/g' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
perl -0777 -pi -e 's/it\.value/it.name/g' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
perl -0777 -pi -e 's/it\.extra/it.description/g' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
