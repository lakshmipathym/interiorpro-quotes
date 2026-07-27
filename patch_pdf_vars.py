import re

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'r') as f:
    content = f.read()

# Fix the totals logic
old_totals = """        // Recalculate totals from items
        val calculatedSubtotal = items.sumOf { it.amount }
        val discount = quotation.discount"""

new_totals = """        // Recalculate totals from quotation values to act as single source of truth for PDF rendering
        val calculatedSubtotal = quotation.subtotal
        val discount = quotation.discount"""

content = content.replace(old_totals, new_totals)

# Add canonicalCompanyName to drawSignature
old_sig = """    private fun drawSignature(
        engine: PdfEngine,
        company: CompanyProfile,
        showCompanySeal: Boolean,
        showSignature: Boolean
    ) {"""

new_sig = """    private fun drawSignature(
        engine: PdfEngine,
        company: CompanyProfile,
        showCompanySeal: Boolean,
        showSignature: Boolean
    ) {
        val canonicalCompanyName = company.companyName.trim().ifBlank { "Company Name" }"""
content = content.replace(old_sig, new_sig)

# Add canonicalCompanyName to drawFooter
old_foot = """    private fun drawFooter(engine: PdfEngine, canvas: Canvas, pageNum: Int, totalPages: Int, company: CompanyProfile, showPageNumber: Boolean) {"""
new_foot = """    private fun drawFooter(engine: PdfEngine, canvas: Canvas, pageNum: Int, totalPages: Int, company: CompanyProfile, showPageNumber: Boolean) {
        val canonicalCompanyName = company.companyName.trim().ifBlank { "Company Name" }"""
content = content.replace(old_foot, new_foot)

with open('app/src/main/java/com/example/pdf/PdfGenerator.kt', 'w') as f:
    f.write(content)
