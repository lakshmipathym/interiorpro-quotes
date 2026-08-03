package com.example.domain.contracts

import com.example.domain.models.CompanySnapshot

interface BrandingAssetCopier {
    suspend fun copyAssetsForQuotation(quotationNumber: String, company: CompanySnapshot): CompanySnapshot
}
