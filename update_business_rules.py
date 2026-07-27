import re

with open("BUSINESS_RULES_V1.5.md", "r") as f:
    content = f.read()

target = """| Rule | Implementation A | Implementation B | Conflict Type | Resolution Required Later |
|---|---|---|---|---|
| Item Qty Drawing | `NewQuotationScreen.kt` calculates Qty -> DB | `PdfGenerator.kt:871` recalculates from raw specs | PDF_RECALCULATION | PDF must consume `item.amount / item.rate` or pass explicit Qty |
| Item Qty Measurement | `PdfGenerator.kt:783` uses `amount / rate` | `PdfGenerator.kt:871` uses `calculateQuantity` | PDF_RECALCULATION | Internal PDF logic conflict |
| Item Amount | `QuotationCalculationEngine.kt:58` | `NewQuotationScreen.kt:2061` | UI_RECALCULATION | Move to Use Case |"""

replacement = """| Rule | Implementation A | Implementation B | Conflict Type | Resolution Required Later |
|---|---|---|---|---|
| **Billable Quantity (3-Way Split)** | **UI**: Calculates billable qty inline for display. <br> **DB**: Stores raw `quantity` (count) not billable qty. <br> **PDF Generator**: Recalculates billable qty again from raw specs. | *All three can diverge if rate or formulas change.* | **DATA_SOURCE_CONFLICT / PDF_RECALCULATION** | **CRITICAL:** DB must store the final calculated `billableQuantity`, and UI/PDF must consume it directly. |
| Item Amount | `QuotationCalculationEngine.kt:58` | `NewQuotationScreen.kt:2061` | UI_RECALCULATION | Move to Use Case |"""

content = content.replace(target, replacement)

with open("BUSINESS_RULES_V1.5.md", "w") as f:
    f.write(content)
