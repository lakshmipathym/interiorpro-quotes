with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "r") as f:
    content = f.read()

target = "val grandTotalRaw = quotation.subtotal - quotation.discount + quotation.gstAmount"
replacement = "val grandTotalRaw = quotation.subtotal - quotation.discount + quotation.gstAmount + quotation.transport + quotation.installation + quotation.extraCharges"

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/pdf/PdfGenerator.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
