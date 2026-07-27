package com.example.data.snapshot

import com.example.data.Quotation
import com.example.data.QuotationItem
import com.example.domain.models.*

object QuotationSnapshotMapper {

    fun toEntity(snapshot: FinalizedQuotationSnapshot): Pair<Quotation, List<QuotationItem>> {
        val quotation = Quotation(
            id = snapshot.id.toIntOrNull() ?: 0,
            quotationNumber = snapshot.quotationNumber,
            date = snapshot.date,
            
            customerId = snapshot.customer.customerId.toLongOrNull() ?: 0L,
            customerName = snapshot.customer.customerName,
            customerPhone = snapshot.customer.customerPhone,
            customerAddress = snapshot.customer.customerAddress,
            siteName = snapshot.customer.siteName,
            siteAddress = snapshot.customer.siteAddress,
            
            companyNameSnapshot = snapshot.company.companyName,
            companyOwnerNameSnapshot = snapshot.company.ownerName,
            companyPhoneSnapshot = snapshot.company.phone,
            companyEmailSnapshot = snapshot.company.email,
            companyAddressSnapshot = snapshot.company.address,
            companyGstinSnapshot = snapshot.company.gstin,
            companyBankNameSnapshot = snapshot.company.bankName,
            companyAccountNameSnapshot = snapshot.company.accountHolderName,
            companyAccountNumberSnapshot = snapshot.company.accountNumber,
            companyIfscSnapshot = snapshot.company.ifsc,
            companyBranchSnapshot = snapshot.company.branch,
            companyUpiIdSnapshot = snapshot.company.upiId,
            
            subtotal = snapshot.financial.subtotal,
            discount = snapshot.financial.discount,
            taxableAmount = snapshot.financial.taxableAmount,
            gstRate = snapshot.financial.gstRate,
            gstAmount = snapshot.financial.gstAmount,
            transport = snapshot.financial.transport,
            installation = snapshot.financial.installation,
            extraCharges = snapshot.financial.extraCharges,
            roundOff = snapshot.financial.roundOff,
            grandTotal = snapshot.financial.grandTotal,
            advance = snapshot.financial.advance,
            balance = snapshot.financial.balanceDue,
            amountInWords = snapshot.financial.amountInWords,
            
            termsAndConditions = snapshot.termsAndConditions,
            warranty = snapshot.warranty,
            validityDays = snapshot.validityDays,
            internalNotes = snapshot.notes
        )
        
        val items = snapshot.items.map { item ->
            QuotationItem(
                id = item.itemId.toIntOrNull() ?: 0,
                quotationId = quotation.id,
                itemName = item.itemName,
                description = item.description,
                material = item.material,
                finish = item.finish,
                rawWidth = item.rawWidth,
                rawHeight = item.rawHeight,
                rawDepth = item.rawDepth,
                parsedWidth = item.parsedWidth,
                parsedHeight = item.parsedHeight,
                parsedDepth = item.parsedDepth,
                rawQuantity = item.quantity,
                billableQuantity = item.billableQuantity,
                quantity = item.billableQuantity,
                unit = item.parsedUnit.name,
                rate = item.rate,
                amount = item.itemAmount
            )
        }
        
        return Pair(quotation, items)
    }

    fun toDomain(quotation: Quotation, items: List<QuotationItem>): FinalizedQuotationSnapshot {
        return FinalizedQuotationSnapshot(
            id = quotation.id.toString(),
            quotationNumber = quotation.quotationNumber,
            date = quotation.date,
            customer = CustomerSnapshot(
                customerId = quotation.customerId.toString(),
                customerName = quotation.customerName,
                customerPhone = quotation.customerPhone,
                customerAddress = quotation.customerAddress,
                siteName = quotation.siteName,
                siteAddress = quotation.siteAddress
            ),
            company = CompanySnapshot(
                companyName = quotation.companyNameSnapshot,
                ownerName = quotation.companyOwnerNameSnapshot,
                phone = quotation.companyPhoneSnapshot,
                email = quotation.companyEmailSnapshot,
                address = quotation.companyAddressSnapshot,
                gstin = quotation.companyGstinSnapshot,
                bankName = quotation.companyBankNameSnapshot,
                accountHolderName = quotation.companyAccountNameSnapshot,
                accountNumber = quotation.companyAccountNumberSnapshot,
                ifsc = quotation.companyIfscSnapshot,
                branch = quotation.companyBranchSnapshot,
                upiId = quotation.companyUpiIdSnapshot
            ),
            financial = FinancialSnapshot(
                subtotal = quotation.subtotal,
                discount = quotation.discount,
                taxableAmount = quotation.taxableAmount,
                gstRate = quotation.gstRate,
                gstAmount = quotation.gstAmount,
                transport = quotation.transport,
                installation = quotation.installation,
                extraCharges = quotation.extraCharges,
                roundOff = quotation.roundOff,
                grandTotal = quotation.grandTotal,
                advance = quotation.advance,
                balanceDue = quotation.balance,
                amountInWords = quotation.amountInWords
            ),
            items = items.map { item ->
                FinalizedItemSnapshot(
                    itemId = item.id.toString(),
                    itemName = item.itemName,
                    description = item.description,
                    material = item.material,
                    finish = item.finish,
                    rawWidth = item.rawWidth,
                    rawHeight = item.rawHeight,
                    rawDepth = item.rawDepth,
                    parsedWidth = item.parsedWidth,
                    parsedHeight = item.parsedHeight,
                    parsedDepth = item.parsedDepth,
                    parsedUnit = try { UnitType.valueOf(item.unit) } catch (e: Exception) { UnitType.SQ_FT },
                    quantity = item.rawQuantity,
                    billableQuantity = item.billableQuantity,
                    rate = item.rate,
                    itemAmount = item.amount
                )
            },
            termsAndConditions = quotation.termsAndConditions,
            warranty = quotation.warranty,
            validityDays = quotation.validityDays,
            notes = quotation.internalNotes
        )
    }
}
