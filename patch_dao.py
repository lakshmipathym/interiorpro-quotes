import re

with open('app/src/main/java/com/example/data/Daos.kt', 'r') as f:
    content = f.read()

bad = """suspend fun getQuotationByNumberDirect(quotationNumber: String): Quotation?
    @Query("SELECT quotationNumber FROM quotation WHERE quotationNumber LIKE :prefix || '%' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestQuotationNumber"""

good = """@Query("SELECT * FROM quotation WHERE quotationNumber = :quotationNumber LIMIT 1")
    suspend fun getQuotationByNumberDirect(quotationNumber: String): Quotation?
    
    @Query("SELECT quotationNumber FROM quotation WHERE quotationNumber LIKE :prefix || '%' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestQuotationNumber"""

content = content.replace(bad, good)

with open('app/src/main/java/com/example/data/Daos.kt', 'w') as f:
    f.write(content)
