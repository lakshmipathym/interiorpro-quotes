# InteriorPro V1.5 - Business Rules Contract

## 1. Business Flow
```text
Quotation Creation -> Customer Selection
  -> Quotation Items
    -> Dimensions (Width, Height, Depth), Quantity, Unit, Rate
      -> Calculation Engine (Total Billable Qty = Area/Vol/Qty formula)
        -> Item Amount = Billable Qty × Rate (Inline in UI)
  -> View Model Totals
    -> Subtotal = Sum of Item Amounts
    -> Taxable Amount = max(0, Subtotal - Discount)
    -> GST = Taxable Amount × (GST Rate / 100)
    -> Grand Total = Taxable Amount + GST + Transport + Installation + Extra Charges + Round Off
    -> Balance = Grand Total - Advance
  -> PDF Generator
    -> Recalculates Billable Qty inline for display
    -> Pulls amount from DB directly
    -> Amount in words based on Grand Total
```

## 2. Dimension Rules
| Input | Parser | Stored As | Converted To | Used By | Risk |
|---|---|---|---|---|---|
| "10 ft", "10'" | `parseDimensionToFeet` | String | Double (Feet) | Area/Vol Calc | None |
| "10' 6"" | `parseDimensionToFeet` | String | 10.5 Feet | Area/Vol Calc | None |
| "10.5" | `parseDimensionToFeet` | String | 10.5 Feet | Area/Vol Calc | None |
| "0", empty | `parseDimensionToFeet` | String | 0.0 Feet | Area/Vol Calc | None |

## 3. Unit Rules
| Unit | Width? | Height? | Depth? | Qty? | Formula | Source | Status |
|---|---|---|---|---|---|---|---|
| Sq.Ft / Sft | Yes | Yes | No | Yes | `W × H × Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Cu.Ft / Cft | Yes | Yes | Yes | Yes | `W × H × D × Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Sq.M / Sqm | Yes | Yes | No | Yes | `W × H × 0.09290304 × Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Cu.M / Cum | Yes | Yes | Yes | Yes | `W × H × D × 0.028316846592 × Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Meter / Mtr | Yes | No | No | Yes | `W × 0.3048 × Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| R.Ft / Run | Yes | No | No | Yes | `W × Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Lumpsum | No | No | No | Yes | `Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Nos / Default | No | No | No | Yes | `Qty` | `QuotationCalculationEngine.kt` | CONFIRMED |

## 4. Quantity & Billable Quantity Rules
| Rule | Source | Formula | Status |
|---|---|---|---|
| Total Billable Qty | `NewQuotationScreen.kt`, `PdfGenerator.kt` | Uses Engine formulas | CONFLICTED (PdfGen measures via `amount / rate` then draws via `calculateQuantity`) |

## 5. Item Amount Rules
| Rule | Formula | Source | Status |
|---|---|---|---|
| Item Amount | `Billable Qty × Rate` | `NewQuotationScreen.kt:2061` | CONFIRMED (but inline instead of Engine) |

## 6. Financial Rules
| Financial Component | Formula | Source | Status |
|---|---|---|---|
| Subtotal | `sum(item.amount)` | `QuotationCalculationEngine.kt` | CONFIRMED |
| Taxable Amount | `max(0, subtotal - discount)` | `TaxEngine.kt` | CONFIRMED |
| GST Amount | `Taxable × (rate / 100)` | `TaxEngine.kt` | CONFIRMED |
| Grand Total | `Taxable + GST + Transport + Installation + Extra + RoundOff` | `QuotationCalculationEngine.kt` | CONFIRMED |

## 7. Grand Total Forensic Trace
- **Location 1**: `QuotationCalculationEngine.kt:86` (`calculateGrandTotal`)
- **Location 2**: `PdfGenerator.kt:932` (`normalizedFinalGrandTotal = Math.round(quotation.grandTotal * 100.0) / 100.0`)
- **Status**: The PDF consumes `quotation.grandTotal` from the DB, but rounds it inline.

## 8. Amount in Words Rules
| Input | Processing | Output | Source | Used By |
|---|---|---|---|---|
| `normalizedFinalGrandTotal` | `CurrencyFormatter.convertNumberToWords` | English string | `PdfGenerator.kt:936` | PDF Generation |

## 9. Company Profile Rules
- Company data is fetched directly from the DB (`CompanyProfileDao`).
- Used as fallback for missing quotation data (Warranty, Validity).

## 10. Terms & Conditions Rules
- `PdfGenerator.kt` dynamically builds a list combining:
  1. `quotation.warranty` fallback to `company.defaultWarranty`
  2. `company.defaultDeliveryTime`
  3. `company.defaultInstallationTime`
  4. `company.defaultPaymentTerms`
  5. `quotation.validityDays` fallback to `company.defaultValidityDays`
  6. `company.additionalConditions`
  7. `quotation.termsAndConditions` (split by newline)
- **Status**: CONFIRMED. Complex assembly happens inside `PdfGenerator`.

## 11. Payment Rules
- UPI ID and Bank Details are fetched from `company` directly in `PdfGenerator`.
- Balance Due = `normalizedFinalGrandTotal - quotation.advance` (`PdfGenerator.kt:933`).

## 12. PDF Data Contract
| PDF Field | Data Source | Recalculated Inside PDF? | Status |
|---|---|---|---|
| Item Qty | `calculateQuantity(w, h, q, unit, d)` | **YES** | CONFLICTED |
| Item Amount | `item.amount` | NO | CONFIRMED |
| Subtotal | `quotation.subtotal` | NO | CONFIRMED |
| Grand Total | `quotation.grandTotal` | NO | CONFIRMED (Rounded) |
| Terms | Mixed (Quote + Company) | **YES** | REFACTOR REQUIRED |

## 13. Conflict Register
| Rule | Implementation A | Implementation B | Conflict Type | Resolution Required Later |
|---|---|---|---|---|
| **Billable Quantity (3-Way Split)** | **UI**: Calculates billable qty inline for display. <br> **DB**: Stores raw `quantity` (count) not billable qty. <br> **PDF Generator**: Recalculates billable qty again from raw specs. | *All three can diverge if rate or formulas change.* | **DATA_SOURCE_CONFLICT / PDF_RECALCULATION** | **CRITICAL:** DB must store the final calculated `billableQuantity`, and UI/PDF must consume it directly. |
| Item Amount | `QuotationCalculationEngine.kt:58` | `NewQuotationScreen.kt:2061` | UI_RECALCULATION | Move to Use Case |

## 14. V1.5 Business Rule Decision Status
- **CONFIRMED**: Dimension parsing, Unit formulas, Tax formulas, Amount in words.
- **CONFLICTED**: PDF Billable Quantity, Item Amount invocation.
- **REFACTOR REQUIRED**: Terms assembly (move to ViewModel or Use Case).
