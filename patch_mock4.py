import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

content = content.replace("val outputFile = File(context.cacheDir, \"test1.pdf\")\n        try {\n            PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)",
"val outputFile = File(context.cacheDir, \"test1.pdf\")\n        try {\n            PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)")

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'w') as f:
    f.write(content)

