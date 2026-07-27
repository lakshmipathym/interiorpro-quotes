package com.example.domain.engine

import com.example.domain.contracts.QuotationSnapshotFactory
import com.example.domain.models.CalculatedQuotation
import com.example.domain.models.CompanySnapshot
import com.example.domain.models.CustomerSnapshot
import com.example.domain.models.FinalizedItemSnapshot
import com.example.domain.models.FinalizedQuotationSnapshot
import com.example.domain.models.FinancialSnapshot
import com.example.domain.models.RawQuotationInput
import java.util.UUID

class QuotationSnapshotFactoryImpl : QuotationSnapshotFactory {

    override fun createSnapshot(
        id: String,
        quotationNumber: String,
        date: Long,
        customer: CustomerSnapshot,
        company: CompanySnapshot,
        termsAndConditions: String,
        warranty: String,
        validityDays: Int,
        notes: String,
        rawInput: RawQuotationInput,
        calculatedQuotation: CalculatedQuotation
    ): FinalizedQuotationSnapshot {

        val itemSnapshots = calculatedQuotation.items.map { calculatedItem ->
            FinalizedItemSnapshot(
                itemId = UUID.randomUUID().toString(),
                itemName = calculatedItem.rawInput.itemName,
                description = calculatedItem.rawInput.description,
                material = calculatedItem.rawInput.material,
                finish = calculatedItem.rawInput.finish,
                rawWidth = calculatedItem.rawInput.width,
                rawHeight = calculatedItem.rawInput.height,
                rawDepth = calculatedItem.rawInput.depth,
                parsedWidth = calculatedItem.parsedWidth,
                parsedHeight = calculatedItem.parsedHeight,
                parsedDepth = calculatedItem.parsedDepth,
                parsedUnit = calculatedItem.parsedUnit,
                quantity = calculatedItem.rawInput.quantity,
                billableQuantity = calculatedItem.billableQuantity,
                rate = calculatedItem.rawInput.rate,
                itemAmount = calculatedItem.itemAmount
            )
        }

        val financialSnapshot = FinancialSnapshot(
            subtotal = calculatedQuotation.subtotal,
            discount = rawInput.discount,
            taxableAmount = calculatedQuotation.taxableAmount,
            gstRate = rawInput.gstRate,
            gstAmount = calculatedQuotation.gstAmount,
            transport = rawInput.transport,
            installation = rawInput.installation,
            extraCharges = rawInput.extraCharges,
            roundOff = rawInput.roundOff,
            grandTotal = calculatedQuotation.grandTotal,
            advance = rawInput.advance,
            balanceDue = calculatedQuotation.balanceDue,
            amountInWords = calculatedQuotation.amountInWords
        )

        return FinalizedQuotationSnapshot(
            id = id,
            quotationNumber = quotationNumber,
            date = date,
            customer = customer,
            company = company,
            items = itemSnapshots,
            financial = financialSnapshot,
            termsAndConditions = termsAndConditions,
            warranty = warranty,
            validityDays = validityDays,
            notes = notes
        )
    }
}
