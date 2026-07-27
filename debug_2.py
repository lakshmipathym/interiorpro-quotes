import sys

with open("app/src/test/java/com/example/QuotationEngineTest.kt", "r") as f:
    content = f.read()

content = content.replace(
    'assertEquals(11400.0, subtotal, 0.001) // 2400 + 9000',
    'System.out.println("Subtotal at 191: " + subtotal + " Items: " + viewModel.newQuoteItems.value); assertEquals(11400.0, subtotal, 0.001) // 2400 + 9000'
)

with open("app/src/test/java/com/example/QuotationEngineTest.kt", "w") as f:
    f.write(content)

