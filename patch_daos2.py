with open('app/src/main/java/com/example/data/Daos.kt', 'r') as f:
    content = f.read()

bad = "    suspend fun getQuotationByNumberDirect(quotationNumber: String): Quotation?"
good = '    @Query("SELECT * FROM quotation WHERE quotationNumber = :quotationNumber LIMIT 1")\n    suspend fun getQuotationByNumberDirect(quotationNumber: String): Quotation?'

content = content.replace(bad, good)

with open('app/src/main/java/com/example/data/Daos.kt', 'w') as f:
    f.write(content)
