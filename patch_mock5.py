import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

old_code = """        val draftOutputFile = File(context.cacheDir, "test2.pdf")
        try {
            PdfGenerator.generateQuotationPdf(context, quotesRepo, 2, draftOutputFile)
        } catch (e: IllegalStateException) {"""

new_code = """        val draftOutputFile = File(context.cacheDir, "test2.pdf")
        try {
            PdfGenerator.generateQuotationPdf(context, quotesRepo, 2, draftOutputFile)
        } catch (e: IllegalStateException) {"""

content = content.replace("PdfGenerator.generateQuotationPdf(context, quotesRepo, 2, draftOutputFile)",
                          "runBlocking { PdfGenerator.generateQuotationPdf(context, quotesRepo, 2, draftOutputFile) }")

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'w') as f:
    f.write(content)

