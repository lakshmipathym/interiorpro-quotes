import sys

file_path = "app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

old_validation = """                    if (qtyVal < 0 || rateVal < 0) {
                        Toast.makeText(context, "Values cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }"""

new_validation = """                    if (itemName.isBlank()) {
                        Toast.makeText(context, "Item Name is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (qtyVal < 0 || rateVal < 0) {
                        Toast.makeText(context, "Values cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }"""

if old_validation in content:
    content = content.replace(old_validation, new_validation)
    with open(file_path, "w") as f:
        f.write(content)
    print("Patched Item Name validation")
else:
    print("Could not find validation")
