import re

with open('app/src/test/java/com/example/pdf/PdfCustomerSnapshotTest.kt', 'r') as f:
    content = f.read()

content = content.replace("PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)",
                          "PdfGenerator.generateQuotationPdf(context, snapshot.company, snapshot, outputFile)")

# Wait, snapshot.company in FinalizedQuotationSnapshot is a CompanySnapshot, not CompanyProfile!

print("FOUND IT. snapshot.company is CompanySnapshot but generateQuotationPdf expects CompanyProfile")
