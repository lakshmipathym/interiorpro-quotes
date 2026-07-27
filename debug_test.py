import sys
import re

with open("app/src/test/java/com/example/QuotationEngineTest.kt", "r") as f:
    content = f.read()

content = content.replace(
    'assertEquals(1000.0, quotationViewModel.newQuoteSubtotal.value, 0.001)',
    'System.out.println("Subtotal 339: " + quotationViewModel.newQuoteSubtotal.value); assertEquals(1000.0, quotationViewModel.newQuoteSubtotal.value, 0.001)'
)

with open("app/src/test/java/com/example/QuotationEngineTest.kt", "w") as f:
    f.write(content)

