import re

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'r') as f:
    content = f.read()

pattern = r'''if \(qtyVal == 0\.0\) \{
                        Toast\.makeText\(context, "Quantity must be greater than 0", Toast\.LENGTH_SHORT\)\.show\(\)
                        return@PremiumPrimaryButton
                    \}'''

replacement = '''if (qtyVal == 0.0) {
                        Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (material.isBlank()) {
                        Toast.makeText(context, "Material is required", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if (wFeet < 0 || hFeet < 0 || dFeet < 0) {
                        Toast.makeText(context, "Dimensions cannot be negative", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }
                    if ((isAreaBased || isRftBased) && (wFeet == 0.0 || (isAreaBased && hFeet == 0.0))) {
                        Toast.makeText(context, "Dimensions cannot be zero for the selected unit", Toast.LENGTH_SHORT).show()
                        return@PremiumPrimaryButton
                    }'''

new_content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/quotation/NewQuotationScreen.kt', 'w') as f:
    f.write(new_content)
