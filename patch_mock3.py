import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

content = content.replace("PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)", 
    "PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)")

print("We found the problem! It's because FinalizedQuotationSnapshot does not match.")
