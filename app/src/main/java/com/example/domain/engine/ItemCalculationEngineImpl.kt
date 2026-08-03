package com.example.domain.engine

import com.example.domain.contracts.DimensionParser
import com.example.domain.contracts.ItemCalculationEngine
import com.example.domain.models.CalculatedItem
import com.example.domain.models.RawItemInput
import com.example.domain.models.UnitType

class ItemCalculationEngineImpl(
    private val dimensionParser: DimensionParser
) : ItemCalculationEngine {

    override fun calculateItem(input: RawItemInput): CalculatedItem {
        val parsedWidth = dimensionParser.parseToFeet(input.width)
        val parsedHeight = dimensionParser.parseToFeet(input.height)
        val parsedDepth = dimensionParser.parseToFeet(input.depth)
        val parsedUnit = UnitType.fromString(input.unit)
        val quantity = if (input.quantity < 0.0) 0.0 else input.quantity
        val rate = if (input.rate < 0.0) 0.0 else input.rate

        val billableQuantity = calculateBillableQuantity(
            parsedWidth, parsedHeight, parsedDepth, parsedUnit, quantity
        )

        // The BUSINESS_RULES_V1.5.md marks "Item Amount" vs UI as CONFLICTED.
        // We implement the abstraction and use the formula: Billable Qty × Rate
        val itemAmount = com.example.utils.CurrencyFormatter.normalizeCurrency(billableQuantity * rate)

        return CalculatedItem(
            rawInput = input,
            parsedWidth = parsedWidth,
            parsedHeight = parsedHeight,
            parsedDepth = parsedDepth,
            parsedUnit = parsedUnit,
            billableQuantity = billableQuantity,
            itemAmount = itemAmount
        )
    }

    private fun calculateBillableQuantity(
        w: Double, h: Double, d: Double, unit: UnitType, qty: Double
    ): Double {
        return when (unit) {
            UnitType.SQ_FT -> w * h * qty
            UnitType.CU_FT -> w * h * d * qty
            UnitType.SQ_M -> w * h * 0.09290304 * qty
            UnitType.CU_M -> w * h * d * 0.028316846592 * qty
            UnitType.METER -> w * 0.3048 * qty
            UnitType.R_FT -> w * qty
            UnitType.LUMPSUM -> qty
            UnitType.NOS -> qty
        }
    }
}
