import sys
import re

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "r") as f:
    content = f.read()

# Regex to find the block from newQuoteSubtotal to newQuoteBalance
pattern = r"val newQuoteSubtotal: StateFlow<Double> = _newQuoteItems\.map \{.*?\.stateIn\(viewModelScope, SharingStarted\.WhileSubscribed\(5000\), 0\.0\)"
match = re.search(pattern, content, re.DOTALL)
if not match:
    print("Could not find calc block")
    sys.exit(1)

new_calc_block = """val calculatedQuotation: StateFlow<CalculatedQuotation> = combine(
        _newQuoteItems,
        _newQuoteDiscount,
        _newQuoteGstRate,
        _newQuoteTransport,
        _newQuoteInstallation,
        _newQuoteExtraCharges,
        _newQuoteRoundOff,
        _newQuoteAdvance
    ) { items, discount, gstRate, transport, installation, extraCharges, roundOff, advance ->
        val rawInput = RawQuotationInput(
            discount = discount,
            gstRate = gstRate,
            transport = transport,
            installation = installation,
            extraCharges = extraCharges,
            roundOff = roundOff,
            advance = advance
        )
        val rawItems = items.map {
            val parts = it.description.split("|||")
            val userDesc = parts[0].trim()
            val specsJson = if (parts.size > 1) parts[1].trim() else "{}"
            var w = "0"; var h = "0"; var d = "0"
            try {
                if (specsJson.startsWith("{") && specsJson.endsWith("}")) {
                    val json = org.json.JSONObject(specsJson)
                    w = json.optString("width", "0")
                    h = json.optString("height", "0")
                    d = json.optString("depth", "0")
                }
            } catch (e: Exception) {}
            RawItemInput(
                itemName = it.itemName,
                description = userDesc,
                material = it.material,
                finish = it.finish,
                width = w,
                height = h,
                depth = d,
                quantity = it.quantity,
                unit = it.unit,
                rate = it.rate
            )
        }
        calculateQuotationUseCase.execute(rawInput, rawItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 
        CalculatedQuotation(emptyList(), 0.0, 0.0, 0.0, 0.0, 0.0, "")
    )

    val newQuoteSubtotal: StateFlow<Double> = calculatedQuotation.map { it.subtotal }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val newQuoteGstAmount: StateFlow<Double> = calculatedQuotation.map { it.gstAmount }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val newQuoteGrandTotal: StateFlow<Double> = calculatedQuotation.map { it.grandTotal }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val newQuoteBalance: StateFlow<Double> = calculatedQuotation.map { it.balanceDue }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val newQuoteItems: StateFlow<List<QuotationItem>> = combine(_newQuoteItems, calculatedQuotation) { raw, calc ->
        if (raw.size == calc.items.size) {
            raw.mapIndexed { index, item ->
                item.copy(
                    billableQuantity = calc.items[index].billableQuantity,
                    amount = calc.items[index].itemAmount
                )
            }
        } else {
            raw
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())"""

content = content[:match.start()] + new_calc_block + content[match.end():]

content = content.replace("val newQuoteItems: StateFlow<List<QuotationItem>> = _newQuoteItems.asStateFlow()\n", "")

with open("app/src/main/java/com/example/ui/quotation/QuotationViewModel.kt", "w") as f:
    f.write(content)

