import re

with open('app/src/main/java/com/example/data/Daos.kt', 'r') as f:
    content = f.read()

# First, fix getQuotationByNumberDirect if it lacks @Query or something.
# It's better to just regex replace the exact location
pattern = r"(\s*)suspend fun getQuotationByNumberDirect\(quotationNumber: String\): Quotation\?(\s*)@Query\(\"SELECT quotationNumber FROM quotation WHERE quotationNumber LIKE :prefix \|\| '%' ORDER BY id DESC LIMIT 1\"\)(\s*)suspend fun getLatestQuotationNumber\(prefix: String\): String\?"

replacement = r"""\1@Query("SELECT * FROM quotation WHERE quotationNumber = :quotationNumber LIMIT 1")
    suspend fun getQuotationByNumberDirect(quotationNumber: String): Quotation?
    
    @Query("SELECT quotationNumber FROM quotation WHERE quotationNumber LIKE :prefix || '%' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestQuotationNumber(prefix: String): String?"""

new_content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/data/Daos.kt', 'w') as f:
    f.write(new_content)
