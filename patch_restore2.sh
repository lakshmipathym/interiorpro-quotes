sed -i '588c\                            \/\/ Save' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
sed -i '589c\                            quotationViewModel.saveQuotation { id ->' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
sed -i '590c\                                savedQuotationId = id' app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt
